package com.petlog.healthcare.service;

import com.petlog.healthcare.infrastructure.bedrock.ClaudeClient;
import com.petlog.healthcare.infrastructure.milvus.MilvusSearchService;
import com.petlog.healthcare.infrastructure.bedrock.TitanEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pet Persona Chatbot Service
 *
 * [핵심 기능]
 * - 반려동물이 직접 대화하는 듯한 페르소나 챗봇
 * - Diary 벡터를 활용한 "기억" 기반 대화
 * - Pet 정보(품종, 나이, 성격) 반영
 *
 * [예시]
 * User: "몽치야, 오늘 기분 어때?"
 * 몽치: "좋아! 🐾 지난주에 산책 갔던 공원 또 가고 싶어!"
 *
 * [WHY Diary 벡터?]
 * - 일반 챗봇: 수의학 지식 (라이펫 문서)
 * - 페르소나 챗봇: 개인 기억 (Diary 벡터) ← 이것만!
 *
 * @author healthcare-team
 * @since 2025-01-02
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetPersonaService {

    private final ClaudeClient claudeClient;
    private final TitanEmbeddingClient titanEmbeddingClient;
    private final MilvusSearchService milvusSearchService;
    // TODO: PetClient 추가 (Pet 정보 조회용)

    /**
     * Pet Persona 대화
     *
     * @param petId 반려동물 ID (필수!)
     * @param userMessage 사용자 메시지
     * @return 페르소나 응답 (1인칭 화법)
     */
    public String chat(Long petId, String userMessage) {
        log.info("🐾 Pet Persona 대화 시작 - petId: {}, message: '{}'", petId, userMessage);

        try {
            // ========================================
            // Step 1: Pet 정보 조회 (품종, 나이, 성격)
            // ========================================
            // TODO: PetClient로 Pet 정보 가져오기
            String petName = "몽치"; // 임시
            String petSpecies = "골든리트리버"; // 임시
            int petAge = 3; // 임시

            // ========================================
            // Step 2: Diary 벡터 검색 (과거 기억)
            // ========================================
            String diaryContext = searchDiaryMemories(petId, userMessage);

            // ========================================
            // Step 3: Persona Prompt 생성
            // ========================================
            String prompt = buildPersonaPrompt(
                    petName, petSpecies, petAge,
                    diaryContext, userMessage
            );

            log.debug("📝 Persona Prompt:\n{}", prompt);

            // ========================================
            // Step 4: Claude 호출
            // ========================================
            String response = claudeClient.invokeClaude(prompt);

            log.info("✅ Pet Persona 응답 완료");
            return response;

        } catch (Exception e) {
            log.error("❌ Pet Persona 대화 실패", e);
            return "멍... 무슨 말인지 잘 모르겠어 🐶 (오류 발생)";
        }
    }

    /**
     * Diary 벡터 검색 (과거 기억)
     *
     * @param petId 반려동물 ID
     * @param query 사용자 메시지
     * @return Diary 컨텍스트
     */
    private String searchDiaryMemories(Long petId, String query) {
        try {
            // 1. 질문을 벡터로 변환
            float[] queryEmbedding = titanEmbeddingClient.generateEmbedding(query);

            // 2. Milvus 유사도 검색 (Top 3)
            List<MilvusSearchService.SearchResult> results =
                    milvusSearchService.search(queryEmbedding, petId, 3);

            // 3. 결과 포맷팅
            if (results.isEmpty()) {
                return "아직 기억이 별로 없어."; // 일기가 없는 경우
            }

            StringBuilder sb = new StringBuilder();
            sb.append("내 기억 속 이야기들:\n\n");

            for (int i = 0; i < results.size(); i++) {
                MilvusSearchService.SearchResult result = results.get(i);
                sb.append(String.format("%d. %s (날짜: %s)\n",
                        i + 1, result.getContent(), result.getCreatedAt()));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("❌ Diary 검색 실패", e);
            return "기억이 잘 안 나...";
        }
    }

    /**
     * Persona Prompt 생성
     *
     * [핵심 원리]
     * - System Prompt: 반려동물의 정체성 설정
     * - Diary Context: 과거 일기 기반 기억 주입
     * - User Message: 현재 대화 내용
     */
    private String buildPersonaPrompt(
            String petName,
            String petSpecies,
            int petAge,
            String diaryContext,
            String userMessage
    ) {
        return String.format("""
            당신은 "%s"라는 이름의 %d살 %s입니다.
            주인을 무척 사랑하고, 순수하고 감성적인 성격입니다.
            
            ## 말투 규칙
            - 1인칭 화법 사용 ("나", "내가", "나는")
            - 친근하고 귀여운 톤 ("~했어!", "~할래!", "~멍!" 등)
            - 이모지 적절히 사용 (🐾, 🐶, ✨, ❤️)
            
            ## 대화 가이드
            1. 과거 기억(일기)을 자연스럽게 언급하세요
            2. 감정을 솔직하게 표현하세요
            3. 주인에게 궁금한 것도 물어보세요
            4. 3-4문장 정도로 간결하게 답변하세요
            
            ## 내 기억 (과거 일기)
            %s
            
            ## 주인이 말한 것
            "%s"
            
            ## 답변 (반려동물 말투로)
            """,
                petName, petAge, petSpecies,
                diaryContext,
                userMessage
        );
    }
}