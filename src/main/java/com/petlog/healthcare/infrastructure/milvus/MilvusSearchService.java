package com.petlog.healthcare.infrastructure.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 검색 서비스 (유사도 검색)
 *
 * [검색 전략]
 * 1. 질문을 Titan Embeddings로 벡터화
 * 2. Milvus COSINE 유사도 검색
 * 3. petId로 필터링
 * 4. 상위 N개 결과 반환
 *
 * @author healthcare-team
 * @since 2025-01-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusSearchService {

    private final MilvusServiceClient milvusClient;

    private static final String COLLECTION_NAME = "diary_vectors";
    private static final int NPROBE = 10;

    /**
     * 유사도 검색
     *
     * @param queryEmbedding 질문 벡터 (1024차원)
     * @param petId 반려동물 ID (필터링)
     * @param topK 상위 K개 결과
     * @return 검색 결과 리스트
     */
    public List<SearchResult> search(float[] queryEmbedding, Long petId, int topK) {
        try {
            // 필터 조건 (petId 일치)
            String expr = String.format("petId == \"%d\"", petId);

            // 검색 파라미터 구성
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withOutFields(List.of("content", "metadata"))
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(toList(queryEmbedding)))
                    .withVectorFieldName("embedding")
                    .withExpr(expr)
                    .withParams("{\"nprobe\":" + NPROBE + "}")
                    .build();

            // 검색 실행
            SearchResults results = milvusClient.search(searchParam).getData();
            SearchResultsWrapper wrapper = new SearchResultsWrapper(results.getResults());

            // 결과 변환
            List<SearchResult> searchResults = new ArrayList<>();
            for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
                SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0, i);
                String content = (String) wrapper.getFieldData("content", 0).get(i);
                String metadata = (String) wrapper.getFieldData("metadata", 0).get(i);

                searchResults.add(new SearchResult(
                        idScore.getLongID(),
                        content,
                        idScore.getScore(),
                        parseCreatedAt(metadata)
                ));
            }

            log.info("✅ Milvus 검색 완료 - petId: {}, 결과: {}개", petId, searchResults.size());
            return searchResults;

        } catch (Exception e) {
            log.error("❌ Milvus 검색 실패 - petId: {}", petId, e);
            return Collections.emptyList();
        }
    }

    /**
     * float[] → List<Float> 변환
     */
    private List<Float> toList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    /**
     * 메타데이터에서 createdAt 추출
     */
    private String parseCreatedAt(String metadata) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(metadata);
            return node.path("createdAt").asText("Unknown");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 검색 결과 DTO
     */
    @Data
    public static class SearchResult {
        private final Long diaryId;
        private final String content;
        private final float score;
        private final String createdAt;
    }
}