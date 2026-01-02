package com.petlog.healthcare.infrastructure.milvus;

import com.petlog.healthcare.domain.entity.DiaryMemory;
import com.petlog.healthcare.domain.repository.DiaryMemoryRepository;
import com.petlog.healthcare.infrastructure.bedrock.TitanEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Milvus Vector Store (Spring AI VectorStore 대체)
 *
 * WHY 직접 구현?
 * - Spring AI VectorStore는 내부 EmbeddingModel 사용
 * - Titan Embeddings를 직접 넣으려면 Low-level API 필요
 * - PersonaChatService에서 사용하는 통합 인터페이스 제공
 *
 * @author healthcare-team
 * @since 2026-01-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusVectorStore {

    private final TitanEmbeddingClient titanEmbeddingClient;
    private final MilvusSearchService milvusSearchService;
    private final DiaryMemoryRepository diaryMemoryRepository;

    /**
     * ✅ PersonaChatService에서 호출하는 메서드
     *
     * 유사한 일기 검색 (벡터 유사도 기반)
     *
     * @param queryText 사용자 질문
     * @param userId 사용자 ID (필터링용)
     * @param petId 반려동물 ID (필터링용)
     * @param topK 상위 K개 결과
     * @return 관련 일기 목록
     */
    public List<DiaryMemory> searchSimilarDiaries(
            String queryText,
            Long userId,
            Long petId,
            int topK
    ) {
        log.info("🔍 Milvus 유사도 검색 시작");
        log.info("   Query: '{}'", truncate(queryText, 50));
        log.info("   userId: {}, petId: {}, topK: {}", userId, petId, topK);

        try {
            // Step 1: 질문을 벡터로 변환
            float[] queryEmbedding = titanEmbeddingClient.generateEmbedding(queryText);
            log.debug("   ✅ 쿼리 벡터 생성 완료 (1024차원)");

            // Step 2: Milvus 유사도 검색
            List<MilvusSearchService.SearchResult> searchResults =
                    milvusSearchService.search(queryEmbedding, petId, topK);

            log.info("   ✅ Milvus 검색 완료: {}개 결과", searchResults.size());

            // Step 3: SearchResult → DiaryMemory 변환
            List<DiaryMemory> diaryMemories = new ArrayList<>();

            for (MilvusSearchService.SearchResult result : searchResults) {
                try {
                    // PostgreSQL에서 DiaryMemory 조회
                    DiaryMemory memory = diaryMemoryRepository.findByDiaryId(result.getDiaryId());

                    if (memory != null) {
                        diaryMemories.add(memory);
                        log.debug("   📄 일기 로드: diaryId={}, score={:.2f}",
                                result.getDiaryId(), result.getScore());
                    } else {
                        log.warn("   ⚠️ DiaryMemory 없음: diaryId={}", result.getDiaryId());
                    }

                } catch (Exception e) {
                    log.error("   ❌ DiaryMemory 조회 실패: diaryId={}",
                            result.getDiaryId(), e);
                }
            }

            log.info("✅ 최종 결과: {}개 DiaryMemory 반환", diaryMemories.size());
            return diaryMemories;

        } catch (Exception e) {
            log.error("❌ Milvus 검색 실패", e);
            return new ArrayList<>(); // 빈 리스트 반환
        }
    }

    /**
     * 벡터 저장 (DiaryVectorService에서 사용)
     *
     * 이 메서드는 이미 MilvusDiaryRepository에서 처리하고 있으므로
     * 여기서는 래핑만 제공
     */
    public void saveDiaryVector(Long diaryId, float[] embedding,
                                Long userId, Long petId, String content) {
        log.info("💾 DiaryMemory 벡터 저장 - diaryId: {}", diaryId);

        // 실제 저장은 MilvusDiaryRepository에서 처리
        // 이 메서드는 필요시 추가 로직을 위한 래퍼
    }

    /**
     * 유틸리티: 텍스트 자르기
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}