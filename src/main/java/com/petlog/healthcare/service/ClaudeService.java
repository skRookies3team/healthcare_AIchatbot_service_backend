package com.petlog.healthcare.service;

import com.petlog.healthcare.infrastructure.bedrock.ClaudeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Claude Service (SimpleFileRag 통합)
 *
 * @author healthcare-team
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    private final ClaudeClient claudeClient;
    private final SimpleFileRagService ragService; // 🔥 새로운 RAG

    /**
     * 일반 챗봇 (파일 기반 RAG)
     */
    public String chat(String message) {
        log.info("💬 챗봇 처리 시작: {}", truncate(message, 50));

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("메시지가 비어있습니다.");
        }

        try {
            // Step 1: RAG 검색
            log.info("🔍 RAG 검색 중...");
            String ragContext = ragService.search(message);

            // Step 2: 프롬프트 생성
            String prompt = buildPrompt(ragContext, message);

            // Step 3: Claude 호출
            log.info("🤖 Claude 호출 중...");
            String response = claudeClient.invokeClaude(prompt);

            log.info("✅ 챗봇 처리 완료");
            return response;

        } catch (Exception e) {
            log.error("❌ 챗봇 처리 실패", e);
            throw new RuntimeException("채팅 처리 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * RAG 프롬프트 생성
     */
    private String buildPrompt(String ragContext, String userMessage) {
        return String.format("""
            당신은 반려동물 건강 전문가입니다.
            
            ## 역할
            - 반려동물 보호자의 건강 상담에 전문적으로 답변
            - 증상 분석 및 조치 방법 안내
            - 병원 방문이 필요한 경우 명확히 권고
            
            ## 참고 자료 (라이펫 건강 문서)
            %s
            
            ## 사용자 질문
            %s
            
            ## 답변 가이드라인
            1. **참고 자료 활용**: 위 라이펫 문서 내용을 기반으로 답변하세요
            2. **출처 명시**: "라이펫 자료에 따르면..." 형태로 언급
            3. **의료 안전**: 
               - 확실하지 않은 진단 금지
               - 약물 처방 절대 금지
               - 응급 증상은 즉시 병원 방문 강조 (⚠️ 표시)
            4. **친절한 한국어**: 전문 용어는 쉽게 설명
            5. **실용적 조언**: 가정 관리 vs 병원 치료 구분
            
            ## 답변 형식
            1. 증상 분석 (간단히)
            2. 가능한 원인 (라이펫 문서 기반)
            3. 가정에서의 조치
            4. ⚠️ 병원 방문이 필요한 경우
            
            답변을 시작하세요:
            """,
                ragContext,
                userMessage
        );
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}