package com.petlog.healthcare.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 하이브리드 RAG 서비스 (최종 완성 버전)
 *
 * 4가지 소스 통합:
 * 1. ✅ 라이펫 50개 문서 (로컬 벡터 검색)
 * 2. ✅ 네이버 지식백과 API
 * 3. ✅ PetMD 실시간 크롤링
 * 4. ✅ 라이펫 실시간 크롤링
 *
 * @author 양승준
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRagService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${pet-health.naver.client-id}")
    private String naverClientId;

    @Value("${pet-health.naver.client-secret}")
    private String naverClientSecret;

    @Value("${pet-health.lifet.base-url}")
    private String lifetBaseUrl;

    @Value("${pet-health.lifet.search-path}")
    private String lifetSearchPath;

    @Value("${pet-health.petmd.base-url}")
    private String petmdBaseUrl;

    @Value("${pet-health.petmd.search-path}")
    private String petmdSearchPath;

    @Value("${pet-health.rag.documents-path}")
    private String documentsPath;

    @Value("${pet-health.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${pet-health.rag.top-k:3}")
    private int topK;

    // 라이펫 50개 문서 (메모리 로드)
    private List<HealthDocument> healthDocuments = new ArrayList<>();

    @PostConstruct
    public void loadHealthDocuments() {
        log.info("📚 라이펫 건강 문서 로딩 시작...");

        try {
            Resource resource = resourceLoader.getResource(documentsPath);

            if (!resource.exists()) {
                log.warn("⚠️ 문서 파일이 없습니다: {}", documentsPath);
                log.warn("   → RAG 없이 크롤링만 사용합니다.");
                return;
            }

            JsonNode jsonArray = objectMapper.readTree(resource.getInputStream());

            for (JsonNode node : jsonArray) {
                HealthDocument doc = HealthDocument.builder()
                        .id(node.path("id").asText())
                        .title(node.path("title").asText())
                        .content(node.path("content").asText())
                        .category(node.path("category").asText())
                        .url(node.path("url").asText())
                        .build();
                healthDocuments.add(doc);
            }

            log.info("✅ 라이펫 문서 로딩 완료: {}개", healthDocuments.size());

        } catch (IOException e) {
            log.error("❌ 라이펫 문서 로딩 실패", e);
            log.warn("   → RAG 없이 크롤링만 사용합니다.");
        }
    }

    /**
     * 하이브리드 RAG 검색 (4소스 병렬)
     */
    public String hybridSearch(String query) {
        log.info("🔍 하이브리드 RAG 검색 시작: '{}'", query);

        try {
            // 1. 라이펫 로컬 문서 검색 (동기)
            List<String> localResults = searchLocalDocuments(query);

            // 2. 병렬 API 호출 (비동기)
            CompletableFuture<List<String>> naverFuture = searchNaverAsync(query);
            CompletableFuture<List<String>> petmdFuture = searchPetMdAsync(query);
            CompletableFuture<List<String>> lifetFuture = searchLifetAsync(query);

            // 3. 모든 작업 완료 대기 (최대 5초)
            CompletableFuture.allOf(naverFuture, petmdFuture, lifetFuture)
                    .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .join();

            // 4. 결과 병합
            List<String> allResults = new ArrayList<>();
            allResults.addAll(localResults);
            allResults.addAll(naverFuture.join());
            allResults.addAll(petmdFuture.join());
            allResults.addAll(lifetFuture.join());

            // 5. 중복 제거 및 포맷팅
            String ragContext = formatRagContext(allResults);

            log.info("✅ 하이브리드 RAG 완료: {}개 소스", allResults.size());
            return ragContext;

        } catch (Exception e) {
            log.error("❌ 하이브리드 RAG 실패", e);
            return "RAG 데이터를 가져올 수 없습니다. 일반 답변을 제공합니다.";
        }
    }

    /**
     * 1. 라이펫 로컬 문서 검색
     */
    private List<String> searchLocalDocuments(String query) {
        log.debug("📄 라이펫 로컬 문서 검색...");

        if (healthDocuments.isEmpty()) {
            log.debug("   → 로컬 문서 없음");
            return List.of();
        }

        List<RankedDocument> rankedDocs = healthDocuments.stream()
                .map(doc -> {
                    double score = calculateSimilarity(query, doc.getContent());
                    return new RankedDocument(doc, score);
                })
                // record의 필드에 직접 접근하거나 score() 메서드를 사용합니다.
                .filter(rd -> rd.score >= similarityThreshold)
                // [수정 포인트] RankedDocument::getScore -> RankedDocument::score
                .sorted(Comparator.comparingDouble(RankedDocument::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        List<String> results = rankedDocs.stream()
                .map(rd -> String.format(
                        "[라이펫 문서] %s (유사도: %.2f)\n%s\n출처: %s",
                        rd.document.getTitle(),
                        rd.score,
                        truncate(rd.document.getContent(), 300),
                        rd.document.getUrl()
                ))
                .collect(Collectors.toList());

        log.debug("   → {}개 문서 발견", results.size());
        return results;
    }

    /**
     * 2. 네이버 지식백과 검색 (API)
     */
    private CompletableFuture<List<String>> searchNaverAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("📚 네이버 지식백과 검색...");

            try {
                String url = "https://openapi.naver.com/v1/search/encyc.json" +
                        "?query=" + query.replace(" ", "+") +
                        "&display=3";

                String response = webClient.get()
                        .uri(url)
                        .header("X-Naver-Client-Id", naverClientId)
                        .header("X-Naver-Client-Secret", naverClientSecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (response == null || response.isEmpty()) {
                    log.debug("   → 네이버 검색 결과 없음");
                    return List.of();
                }

                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("items");

                if (items.isEmpty()) {
                    log.debug("   → 네이버 검색 결과 없음");
                    return List.of();
                }

                List<String> results = new ArrayList<>();
                for (JsonNode item : items) {
                    String title = removeHtmlTags(item.path("title").asText());
                    String description = removeHtmlTags(item.path("description").asText());
                    String link = item.path("link").asText();

                    results.add(String.format(
                            "[네이버 지식백과] %s\n%s\n출처: %s",
                            title, truncate(description, 200), link
                    ));
                }

                log.debug("   → {}개 결과 발견", results.size());
                return results;

            } catch (Exception e) {
                log.warn("   → 네이버 검색 실패: {}", e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * 3. PetMD 크롤링 (영어 전문 자료)
     */
    private CompletableFuture<List<String>> searchPetMdAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("🌎 PetMD 크롤링...");

            try {
                String searchUrl = petmdBaseUrl + petmdSearchPath + query.replace(" ", "+");

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(5000)
                        .get();

                // PetMD HTML 구조에 맞게 선택자 수정
                Elements titleElements = doc.select("h2.result-title a, .search-result-title a, h3 a");
                Elements descElements = doc.select(".result-description, .search-result-description, p");

                if (titleElements.isEmpty()) {
                    log.debug("   → PetMD 크롤링 결과 없음");
                    return List.of();
                }

                String title = titleElements.first().text();
                String summary = descElements.isEmpty() ? "최신 정보" : descElements.first().text();

                if (summary.isEmpty() || summary.equals("최신 정보")) {
                    log.debug("   → PetMD 본문 없음");
                    return List.of();
                }

                String result = String.format(
                        "[PetMD 최신] %s\n%s",
                        title, truncate(summary, 200)
                );

                log.debug("   → 크롤링 성공");
                return List.of(result);

            } catch (Exception e) {
                log.warn("   → PetMD 크롤링 실패: {}", e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * 4. 라이펫 실시간 크롤링 (최신 글)
     */
    private CompletableFuture<List<String>> searchLifetAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("🐾 라이펫 실시간 크롤링...");

            try {
                String searchUrl = lifetBaseUrl + lifetSearchPath + query.replace(" ", "+");

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(5000)
                        .get();

                // 라이펫 HTML 구조에 맞게 선택자 수정
                Elements titleElements = doc.select("h2.entry-title a, .post-title a, article h2 a");
                Elements summaryElements = doc.select(".entry-summary, .post-excerpt, article p");

                if (titleElements.isEmpty()) {
                    log.debug("   → 라이펫 크롤링 결과 없음");
                    return List.of();
                }

                String title = titleElements.first().text();
                String summary = summaryElements.isEmpty() ? "최신 정보" : summaryElements.first().text();

                if (summary.isEmpty() || summary.equals("최신 정보")) {
                    log.debug("   → 라이펫 본문 없음");
                    return List.of();
                }

                String result = String.format(
                        "[라이펫 최신] %s\n%s",
                        title, truncate(summary, 200)
                );

                log.debug("   → 크롤링 성공");
                return List.of(result);

            } catch (Exception e) {
                log.warn("   → 라이펫 크롤링 실패: {}", e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * RAG 컨텍스트 포맷팅
     */
    private String formatRagContext(List<String> results) {
        if (results.isEmpty()) {
            return "관련 자료를 찾을 수 없습니다. 일반적인 정보를 기반으로 답변합니다.";
        }

        return String.join("\n\n---\n\n", results);
    }

    /**
     * 텍스트 유사도 계산 (간단한 단어 매칭)
     */
    private double calculateSimilarity(String query, String document) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        String docLower = document.toLowerCase();

        long matchCount = Arrays.stream(queryWords)
                .filter(docLower::contains)
                .count();

        return (double) matchCount / queryWords.length;
    }

    /**
     * HTML 태그 제거
     */
    private String removeHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * 텍스트 자르기
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 건강 문서 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class HealthDocument {
        private String id;
        private String title;
        private String content;
        private String category;
        private String url;
    }

    /**
     * 랭킹된 문서 (유사도 포함)
     */
    private record RankedDocument(HealthDocument document, double score) {}
}