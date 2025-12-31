package com.petlog.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 간단한 파일 기반 RAG 서비스 (Gemini File Search 스타일)
 *
 * 핵심 원리:
 * 1. 애플리케이션 시작 시 JSON 파일 전체를 메모리에 로드
 * 2. 사용자 질문 → 키워드 추출 → 문서 매칭
 * 3. 상위 N개 문서를 RAG 컨텍스트로 반환
 *
 * @author healthcare-team
 * @since 2025-12-31
 */
@Slf4j
@Service
public class SimpleFileRagService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 메모리에 로드된 문서
    private List<HealthDoc> documents = new ArrayList<>();
    private boolean isReady = false;

    /**
     * 애플리케이션 시작 시 문서 로드
     */
    @PostConstruct
    public void init() {
        log.info("════════════════════════════════════════");
        log.info("📚 SimpleFileRagService 초기화 시작");
        log.info("════════════════════════════════════════");

        try {
            loadDocumentsFromClasspath();
            log.info("✅ RAG 시스템 준비 완료: {}개 문서", documents.size());
            isReady = true;
        } catch (Exception e) {
            log.error("❌ RAG 초기화 실패", e);
            isReady = false;
        }

        log.info("════════════════════════════════════════");
    }

    /**
     * 클래스패스에서 JSON 파일 로드
     */
    private void loadDocumentsFromClasspath() {
        String[] possiblePaths = {
                "/rag/lifet_health_docs.json",
                "rag/lifet_health_docs.json",
                "classpath:rag/lifet_health_docs.json"
        };

        for (String path : possiblePaths) {
            try {
                log.info("   시도 중: {}", path);
                InputStream is = getClass().getResourceAsStream(path);

                if (is == null) {
                    log.debug("      → 파일 없음");
                    continue;
                }

                log.info("   ✓ 파일 발견!");
                JsonNode root = objectMapper.readTree(is);

                if (!root.isArray()) {
                    log.error("      → JSON 형식 오류 (배열이 아님)");
                    continue;
                }

                log.info("   파싱 중: {}개 항목", root.size());

                for (JsonNode node : root) {
                    HealthDoc doc = new HealthDoc();
                    doc.id = node.path("id").asText();
                    doc.title = node.path("title").asText();
                    doc.content = node.path("content").asText();
                    doc.category = node.path("category").asText();

                    // 키워드 파싱
                    JsonNode keywordsNode = node.path("keywords");
                    if (keywordsNode.isArray()) {
                        for (JsonNode kw : keywordsNode) {
                            doc.keywords.add(kw.asText().toLowerCase());
                        }
                    }

                    documents.add(doc);
                }

                log.info("   ✅ 로드 완료: {}개 문서", documents.size());

                // 처음 3개 문서 미리보기
                for (int i = 0; i < Math.min(3, documents.size()); i++) {
                    HealthDoc d = documents.get(i);
                    log.info("      [{}] {} (키워드: {})",
                            i+1, d.title, String.join(", ", d.keywords));
                }

                return; // 성공하면 종료

            } catch (Exception e) {
                log.debug("      → 실패: {}", e.getMessage());
            }
        }

        // 모든 경로 실패
        log.error("❌ 모든 경로에서 파일을 찾을 수 없습니다!");
        log.error("   다음 위치를 확인하세요:");
        log.error("   - src/main/resources/rag/lifet_health_docs.json");
        log.error("   - build/resources/main/rag/lifet_health_docs.json");
    }

    /**
     * 🔥 메인 RAG 검색 (Gemini File Search 스타일)
     */
    public String search(String query) {
        log.info("═══════════════════════════════════════");
        log.info("🔍 RAG 검색 시작");
        log.info("═══════════════════════════════════════");
        log.info("   질문: '{}'", query);
        log.info("   준비 상태: {}", isReady ? "✅ 준비됨" : "❌ 준비 안 됨");

        if (!isReady || documents.isEmpty()) {
            log.warn("   ⚠️ 문서가 로드되지 않았습니다");
            return "RAG 시스템이 준비되지 않았습니다.";
        }

        // 1. 질문에서 키워드 추출
        Set<String> queryKeywords = extractKeywords(query);
        log.info("   추출된 키워드: {}", queryKeywords);

        // 2. 각 문서와 유사도 계산
        List<ScoredDoc> scored = new ArrayList<>();

        for (HealthDoc doc : documents) {
            double score = calculateScore(queryKeywords, doc);
            if (score > 0.1) { // 최소 임계값
                scored.add(new ScoredDoc(doc, score));
            }
        }

        // 3. 점수 순으로 정렬
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 상위 5개만
        List<ScoredDoc> topDocs = scored.stream()
                .limit(5)
                .collect(Collectors.toList());

        log.info("   매칭된 문서: {}개", scored.size());
        log.info("   상위 문서:");
        for (ScoredDoc sd : topDocs) {
            log.info("      - {} (점수: {:.2f})", sd.doc.title, sd.score);
        }

        // 4. RAG 컨텍스트 생성
        String ragContext = buildContext(topDocs);

        log.info("   RAG 컨텍스트: {}자", ragContext.length());
        log.info("═══════════════════════════════════════");

        return ragContext;
    }

    /**
     * 질문에서 키워드 추출
     */
    private Set<String> extractKeywords(String query) {
        Set<String> keywords = new HashSet<>();

        // 기본 단어 분리
        String[] words = query.toLowerCase()
                .replaceAll("[^가-힣a-z0-9\\s]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() >= 2) { // 2글자 이상만
                keywords.add(word);
            }
        }

        // 동의어 확장
        Map<String, String[]> synonyms = Map.of(
                "눈곱", new String[]{"눈물", "눈물자국", "눈"},
                "설사", new String[]{"묽은변", "소화불량", "장염"},
                "구토", new String[]{"토", "역류"},
                "기침", new String[]{"켁켁", "호흡곤란"},
                "다리", new String[]{"절뚝", "파행", "보행"},
                "소변", new String[]{"혈뇨", "방광", "요로"},
                "눈", new String[]{"시력", "충혈", "혼탁"}
        );

        Set<String> expanded = new HashSet<>(keywords);
        for (String kw : keywords) {
            for (Map.Entry<String, String[]> entry : synonyms.entrySet()) {
                if (kw.contains(entry.getKey()) || entry.getKey().contains(kw)) {
                    expanded.addAll(Arrays.asList(entry.getValue()));
                }
            }
        }

        return expanded;
    }

    /**
     * 유사도 점수 계산
     *
     * 점수 = (제목 매칭 × 3) + (키워드 매칭 × 2) + (본문 매칭 × 1)
     */
    private double calculateScore(Set<String> queryKeywords, HealthDoc doc) {
        double score = 0.0;

        String titleLower = doc.title.toLowerCase();
        String contentLower = doc.content.toLowerCase();

        for (String qk : queryKeywords) {
            // 제목에서 발견 (가중치 3)
            if (titleLower.contains(qk)) {
                score += 3.0;
            }

            // 문서 키워드에서 발견 (가중치 2)
            for (String docKeyword : doc.keywords) {
                if (docKeyword.contains(qk) || qk.contains(docKeyword)) {
                    score += 2.0;
                    break;
                }
            }

            // 본문에서 발견 (가중치 1)
            if (contentLower.contains(qk)) {
                score += 1.0;
            }
        }

        // 정규화 (0~1 범위)
        return Math.min(1.0, score / (queryKeywords.size() * 6.0));
    }

    /**
     * RAG 컨텍스트 문자열 생성
     */
    private String buildContext(List<ScoredDoc> topDocs) {
        if (topDocs.isEmpty()) {
            return "관련 자료를 찾을 수 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("다음은 라이펫 건강 문서에서 찾은 관련 정보입니다:\n\n");

        for (int i = 0; i < topDocs.size(); i++) {
            ScoredDoc sd = topDocs.get(i);
            sb.append(String.format("[문서 %d] %s (관련도: %.0f%%)\n",
                    i+1, sd.doc.title, sd.score * 100));
            sb.append(truncate(sd.doc.content, 400));
            sb.append("\n\n");

            if (i < topDocs.size() - 1) {
                sb.append("---\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 텍스트 자르기
     */
    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 건강 문서 (간단한 POJO)
     */
    private static class HealthDoc {
        String id;
        String title;
        String content;
        String category;
        List<String> keywords = new ArrayList<>();
    }

    /**
     * 점수가 매겨진 문서
     */
    private static class ScoredDoc {
        HealthDoc doc;
        double score;

        ScoredDoc(HealthDoc doc, double score) {
            this.doc = doc;
            this.score = score;
        }
    }
}