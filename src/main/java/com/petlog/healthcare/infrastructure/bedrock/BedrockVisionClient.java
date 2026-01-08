package com.petlog.healthcare.infrastructure.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Base64;

/**
 * AWS Bedrock Vision Client (멀티모달)
 *
 * Claude 모델을 사용하여 이미지 분석
 * - 반려동물 피부질환 탐지
 * - 이미지 + 텍스트 프롬프트 처리
 *
 * SDK 버전 호환을 위해 InvokeModel API 사용
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockVisionClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    // Claude 3.5 Sonnet (Vision 지원)
    private static final String MODEL_ID = "anthropic.claude-3-5-sonnet-20240620-v1:0";

    // 피부질환 분석 프롬프트
    private static final String SKIN_DISEASE_PROMPT = """
            당신은 수의사 AI 어시스턴트입니다. 반려동물 피부 사진을 분석하여 잠재적인 피부 질환을 식별해주세요.

            분석 항목:
            1. 발견된 증상 (미란, 결절, 궤양, 탈모, 발적, 비듬 등)
            2. 가능한 질환명
            3. 심각도 (경미/중등도/심각)
            4. 권장 조치 (관찰/자가케어/병원방문권장/응급)
            5. 주의사항

            JSON 형식으로 응답해주세요:
            {
                "symptoms": ["증상1", "증상2"],
                "possibleDiseases": ["질환1", "질환2"],
                "severity": "경미|중등도|심각",
                "recommendation": "권장조치",
                "notes": "추가 설명"
            }

            주의: 이 분석은 참고용이며, 정확한 진단은 수의사와 상담하세요.
            """;

    /**
     * 반려동물 피부 이미지 분석
     *
     * @param imageBytes 이미지 바이트 배열
     * @param mediaType  이미지 타입 (image/jpeg, image/png)
     * @return AI 분석 결과 (JSON 문자열)
     */
    public String analyzeSkinImage(byte[] imageBytes, String mediaType) {
        log.info("🔍 피부질환 이미지 분석 시작");
        log.info("   이미지 크기: {} bytes, 타입: {}", imageBytes.length, mediaType);

        try {
            long startTime = System.currentTimeMillis();

            // Base64 인코딩
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Claude Messages API 형식의 요청 생성
            String requestBody = buildRequestBody(base64Image, mediaType);

            // InvokeModel 요청
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);

            // 응답 파싱
            String responseBody = response.body().asUtf8String();
            String result = extractTextFromResponse(responseBody);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 피부질환 분석 완료 ({}ms)", elapsed);

            return result;

        } catch (Exception e) {
            log.error("❌ 피부질환 분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 분석 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Claude Messages API 요청 바디 생성
     */
    private String buildRequestBody(String base64Image, String mediaType) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("anthropic_version", "bedrock-2023-05-31");
        root.put("max_tokens", 4096);

        // messages 배열
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // content 배열 (이미지 + 텍스트)
        ArrayNode content = message.putArray("content");

        // 이미지 블록
        ObjectNode imageContent = content.addObject();
        imageContent.put("type", "image");
        ObjectNode source = imageContent.putObject("source");
        source.put("type", "base64");
        source.put("media_type", mediaType != null ? mediaType : "image/jpeg");
        source.put("data", base64Image);

        // 텍스트 블록
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", SKIN_DISEASE_PROMPT);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Claude 응답에서 텍스트 추출
     */
    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.get("content");

        if (content != null && content.isArray() && content.size() > 0) {
            JsonNode firstContent = content.get(0);
            if (firstContent.has("text")) {
                return firstContent.get("text").asText();
            }
        }

        return responseBody;
    }
}
