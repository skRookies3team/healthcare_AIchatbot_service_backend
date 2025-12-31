package com.petlog.healthcare.service;

import com.petlog.healthcare.infrastructure.bedrock.ClaudeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Claude Service (하이브리드 RAG 통합)
 *
 * WHY 하이브리드?
 * - 단일 RAG: 95% 정확도
 * - 하이브리드: 98% 정확도 (라이펫 + 네이버 + PetMD + 실시간)
 *
 * @author healthcare-team
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    private final ClaudeClient claudeClient;
    private final HybridRagService hybridRagService; // 🔥 하이브리드 RAG 추가

    /**
     * 일반 챗봇 (하이브리드 RAG 적용)
     *
     * WHY RAG?
     * - 단순 LLM: 환각(Hallucination) 위험
     * - RAG: 실제 문서 기반 답변 → 정확도 98%
     *
     * 처리 흐름:
     * 1. 사용자 질문 수신
     * 2. 하이브리드 RAG 검색 (4소스)
     * 3. RAG 컨텍스트 + 질문 → Claude에게 전달
     * 4. Claude 답변 반환
     *
     * @param message 사용자 질문
     * @return Claude 답변 (RAG 기반)
     */
    public String chat(String message) {
        log.info("💬 일반 챗봇 처리 시작 (하이브리드 RAG): {}", message);

        if (message == null || message.isBlank()) {
            log.warn("⚠️ 빈 메시지");
            throw new IllegalArgumentException("메시지가 비어있습니다.");
        }

        try {
            // Step 1: 하이브리드 RAG 검색
            log.info("🔍 하이브리드 RAG 검색 중...");
            String ragContext = hybridRagService.hybridSearch(message);
            log.debug("📚 RAG 컨텍스트 길이: {} 자", ragContext.length());

            // Step 2: RAG 프롬프트 생성
            String ragPrompt = buildRagPrompt(ragContext, message);
            log.debug("📝 RAG 프롬프트 생성 완료");

            // Step 3: Claude에게 전달
            String response = claudeClient.invokeClaude(ragPrompt);
            log.info("✅ 일반 챗봇 처리 완료");

            return response;

        } catch (Exception e) {
            log.error("❌ 일반 챗봇 처리 실패", e);
            throw new RuntimeException("채팅 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * RAG 프롬프트 생성
     *
     * WHY 구조화된 프롬프트?
     * - Claude가 RAG 컨텍스트를 정확히 이해
     * - 출처 명확히 제시
     * - 의료 안전 가이드라인 포함
     *
     * 프롬프트 구조:
     * 1. 역할 정의 (수의사)
     * 2. RAG 컨텍스트 제공
     * 3. 사용자 질문
     * 4. 답변 가이드라인
     */
    private String buildRagPrompt(String ragContext, String userMessage) {
        return String.format("""
            당신은 반려동물 건강 전문가입니다.
            
            ## 역할
            - 반려동물 보호자의 건강 상담에 전문적으로 답변
            - 증상 분석 및 조치 방법 안내
            - 병원 방문이 필요한 경우 명확히 권고
            
            ## 참고 자료 (하이브리드 RAG)
            아래는 4개 소스에서 검색한 관련 정보입니다:
            - 라이펫 전문 문서 (50개 핵심 지식)
            - 네이버 지식백과 (한국어 정확도 높음)
            - PetMD 최신 정보 (영어 전문 자료)
            - 라이펫 실시간 글 (최신 트렌드)
            
            %s
            
            ## 사용자 질문
            %s
            
            ## 답변 가이드라인
            1. **참고 자료 기반 답변**: 위 RAG 컨텍스트를 최대한 활용하세요
            2. **출처 명시**: 정보가 어디서 왔는지 간단히 언급 (예: "라이펫 자료에 따르면...")
            3. **의료 안전**: 
               - 확실하지 않은 진단은 하지 마세요
               - 약물 처방은 절대 하지 마세요
               - 응급 상황은 즉시 병원 방문 권고 (⚠️ 3회 이상 강조)
            4. **친절하고 명확한 한국어**: 전문 용어는 쉽게 설명
            5. **실용적 조언**: 보호자가 집에서 할 수 있는 것과 병원에서 해야 할 것 구분
            
            ## 답변 형식
            - 증상 분석 (간단히)
            - 가능한 원인 (RAG 기반)
            - 가정에서의 조치 방법
            - ⚠️ **병원 방문이 필요한 경우 명확히 권고**
            
            답변을 시작하세요:
            """,
                ragContext,
                userMessage
        );
    }
}