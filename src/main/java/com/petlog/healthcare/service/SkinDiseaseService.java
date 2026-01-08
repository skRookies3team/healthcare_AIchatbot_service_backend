package com.petlog.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.healthcare.dto.skindisease.SkinDiseaseResponse;
import com.petlog.healthcare.dto.skindisease.SkinDiseaseResponse.AnalysisResult;
import com.petlog.healthcare.infrastructure.bedrock.BedrockVisionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 반려동물 피부질환 분석 서비스
 *
 * AWS Bedrock Claude Vision을 사용하여 이미지 분석
 * 분석된 이미지는 S3에 저장
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkinDiseaseService {

    private final BedrockVisionClient bedrockVisionClient;
    private final ObjectMapper objectMapper;
    private final S3ImageService s3ImageService;

    /**
     * 피부질환 이미지 분석
     *
     * @param image 업로드된 이미지 파일
     * @return 분석 결과
     */
    public SkinDiseaseResponse analyzeImage(MultipartFile image) {
        log.info("🔬 피부질환 분석 요청");
        log.info("   파일명: {}, 크기: {} bytes",
                image.getOriginalFilename(), image.getSize());

        try {
            // 이미지 유효성 검사
            validateImage(image);

            // AI 분석 호출
            String rawResponse = bedrockVisionClient.analyzeSkinImage(
                    image.getBytes(),
                    image.getContentType());

            // JSON 파싱
            AnalysisResult result = parseResponse(rawResponse);

            // S3에 이미지 저장 (실패해도 분석 결과는 반환)
            String imageUrl = null;
            try {
                imageUrl = s3ImageService.uploadImage(image, "skin-disease");
                if (imageUrl != null) {
                    log.info("📸 이미지 S3 저장 완료: {}", imageUrl);
                }
            } catch (Exception s3Error) {
                log.warn("⚠️ S3 업로드 실패 (분석은 정상 완료): {}", s3Error.getMessage());
            }

            log.info("✅ 피부질환 분석 완료 - 심각도: {}", result.getSeverity());
            return SkinDiseaseResponse.success(result, imageUrl);

        } catch (Exception e) {
            log.error("❌ 피부질환 분석 실패: {}", e.getMessage(), e);
            return SkinDiseaseResponse.error("분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 이미지 유효성 검사
     */
    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 비어있습니다.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 최대 10MB
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 크기는 10MB 이하여야 합니다.");
        }
    }

    /**
     * AI 응답 파싱
     */
    private AnalysisResult parseResponse(String rawResponse) {
        try {
            // JSON 블록 추출 (코드 블록 내부)
            String json = extractJson(rawResponse);
            JsonNode node = objectMapper.readTree(json);

            return AnalysisResult.builder()
                    .symptoms(parseStringList(node.get("symptoms")))
                    .possibleDiseases(parseStringList(node.get("possibleDiseases")))
                    .severity(getTextOrDefault(node.get("severity"), "알 수 없음"))
                    .recommendation(getTextOrDefault(node.get("recommendation"), "수의사 상담 권장"))
                    .notes(getTextOrDefault(node.get("notes"), ""))
                    .rawResponse(rawResponse)
                    .build();

        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 원본 응답 반환: {}", e.getMessage());
            return AnalysisResult.builder()
                    .symptoms(List.of())
                    .possibleDiseases(List.of())
                    .severity("분석 필요")
                    .recommendation("수의사 상담 권장")
                    .notes(rawResponse)
                    .rawResponse(rawResponse)
                    .build();
        }
    }

    /**
     * JSON 블록 추출
     */
    private String extractJson(String text) {
        // ```json ... ``` 블록 추출
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * JsonNode를 List<String>으로 변환
     */
    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    /**
     * JsonNode에서 텍스트 추출 (기본값 지원)
     */
    private String getTextOrDefault(JsonNode node, String defaultValue) {
        return node != null && !node.isNull() ? node.asText() : defaultValue;
    }
}
