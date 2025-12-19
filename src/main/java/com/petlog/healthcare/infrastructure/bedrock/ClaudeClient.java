package com.petlog.healthcare.infrastructure.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.healthcare.config.BedrockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * AWS Bedrock Claude API Client
 *
 * Claude 3.5 Haiku 모델을 호출하여 AI 응답을 생성하는 클라이언트
 *
 * 주요 기능:
 * - Claude API 호출 (InvokeModel)
 * - JSON 요청/응답 파싱
 * - 에러 핸들링
 *
 * WHY infrastructure 패키지?
 * - 외부 시스템(AWS Bedrock)과의 통신 담당
 * - Domain과 분리된 Infrastructure Layer
 * - DDD 아키텍처 패턴 적용
 *
 * @author healthcare-team
 * @since 2025-12-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final BedrockProperties bedrockProperties;
    private final ObjectMapper objectMapper;

    /**
     * Claude 3.5 Haiku 모델 호출
     *
     * @param userMessage 사용자 입력 메시지
     * @return Claude의 응답 텍스트
     *
     * WHY InvokeModel?
     * - AWS Bedrock의 표준 API
     * - 동기 방식 (응답 대기)
     * - InvokeModelWithResponseStream: 스트리밍 방식 (추후 구현 가능)
     */
    public String invokeClaude(String userMessage) {
        log.info("Invoking Claude with message: {}", userMessage);

        try {
            // 1. 요청 JSON 생성
            String requestBody = buildRequestBody(userMessage);
            log.debug("Request body: {}", requestBody);

            // 2. Bedrock API 호출
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(bedrockProperties.getModelId())
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);

            // 3. 응답 JSON 파싱
            String responseBody = response.body().asUtf8String();
            log.debug("Response body: {}", responseBody);

            String claudeResponse = parseResponse(responseBody);
            log.info("Claude response: {}", claudeResponse);

            return claudeResponse;

        } catch (Exception e) {
            log.error("Failed to invoke Claude", e);
            throw new RuntimeException("Claude API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Claude API 요청 Body 생성
     *
     * Claude Messages API 형식:
     * {
     *   "anthropic_version": "bedrock-2023-05-31",
     *   "max_tokens": 1000,
     *   "messages": [
     *     {
     *       "role": "user",
     *       "content": "사용자 메시지"
     *     }
     *   ]
     * }
     *
     * @param userMessage 사용자 입력
     * @return JSON 형식의 요청 Body
     *
     * WHY Messages API?
     * - Claude 3.x 모델의 표준 인터페이스
     * - 다중 턴 대화 지원
     * - System Prompt 지원 (추후 추가 가능)
     */
    private String buildRequestBody(String userMessage) {
        try {
            return objectMapper.writeValueAsString(
                    new RequestBody(
                            "bedrock-2023-05-31",
                            bedrockProperties.getMaxTokens(),
                            new Message[]{
                                    new Message("user", userMessage)
                            }
                    )
            );
        } catch (Exception e) {
            log.error("Failed to build request body", e);
            throw new RuntimeException("요청 JSON 생성 실패", e);
        }
    }

    /**
     * Claude API 응답 파싱
     *
     * Claude 응답 형식:
     * {
     *   "id": "msg_...",
     *   "type": "message",
     *   "role": "assistant",
     *   "content": [
     *     {
     *       "type": "text",
     *       "text": "Claude의 응답 텍스트"
     *     }
     *   ],
     *   "stop_reason": "end_turn"
     * }
     *
     * @param responseBody JSON 응답
     * @return Claude의 응답 텍스트
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content").get(0);
            return content.path("text").asText();

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            throw new RuntimeException("응답 JSON 파싱 실패", e);
        }
    }

    /**
     * 요청 Body DTO (내부 클래스)
     *
     * WHY 내부 클래스?
     * - ClaudeClient 내부에서만 사용
     * - API 요청 형식이 변경되어도 영향 범위 최소화
     */
    private record RequestBody(
            String anthropic_version,
            Integer max_tokens,
            Message[] messages
    ) {}

    private record Message(
            String role,
            String content
    ) {}
}
