package com.petlog.healthcare.service;

import com.petlog.healthcare.dto.vet.VetQAResult;
import com.petlog.healthcare.entity.VetKnowledge;
import com.petlog.healthcare.repository.VetKnowledgeRepository;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 수의사 지식 베이스 RAG 검색 서비스
 * WHY: Milvus에서 시맨틱 검색으로 관련 Q&A 찾기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VetKnowledgeSearchService {

    private final MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;
    private final VetKnowledgeRepository vetKnowledgeRepository;

    private static final String COLLECTION_NAME = "vet_knowledge";
    private static final int DEFAULT_TOP_K = 5;
    private static final float MIN_SIMILARITY_SCORE = 0.5f;

    /**
     * ⭐ 시맨틱 검색 - 관련 Q&A 찾기
     *
     * @param query      사용자 질문
     * @param department 진료과 필터 (null이면 전체)
     * @param topK       상위 K개 결과
     * @return 관련 Q&A 목록
     */
    public List<VetQAResult> searchRelevantQA(String query, String department, int topK) {
        log.info("🔍 수의사 지식 검색: query='{}', department={}, topK={}",
                truncate(query, 50), department, topK);

        try {
            // 1. 질문 임베딩 생성
            float[] queryEmbedding = generateEmbedding(query);

            // 2. Milvus 검색
            List<SearchResultItem> searchResults = searchMilvus(queryEmbedding, department, topK);

            if (searchResults.isEmpty()) {
                log.info("❌ 검색 결과 없음");
                return Collections.emptyList();
            }

            // 3. PostgreSQL에서 상세 정보 조회
            List<Long> knowledgeIds = searchResults.stream()
                    .map(r -> r.knowledgeId)
                    .collect(Collectors.toList());

            List<VetKnowledge> knowledgeList = vetKnowledgeRepository.findAllById(knowledgeIds);

            // 4. 결과 조합 및 정렬
            Map<Long, VetKnowledge> knowledgeMap = knowledgeList.stream()
                    .collect(Collectors.toMap(VetKnowledge::getId, v -> v));

            List<VetQAResult> results = searchResults.stream()
                    .filter(r -> knowledgeMap.containsKey(r.knowledgeId))
                    .map(r -> {
                        VetKnowledge k = knowledgeMap.get(r.knowledgeId);
                        return VetQAResult.builder()
                                .id(k.getId())
                                .department(k.getDepartment())
                                .disease(k.getDisease())
                                .lifeCycle(k.getLifeCycle())
                                .question(k.getQuestion())
                                .answer(k.getAnswer())
                                .similarityScore(r.score)
                                .build();
                    })
                    .sorted((a, b) -> Float.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                    .collect(Collectors.toList());

            log.info("✅ 검색 완료: {}개 결과", results.size());
            return results;

        } catch (Exception e) {
            log.error("❌ 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * RAG Context 생성 (챗봇용)
     */
    public String buildRAGContext(String query, String department, int topK) {
        List<VetQAResult> results = searchRelevantQA(query, department, topK);

        if (results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("📚 관련 수의사 지식 베이스:\n\n");

        for (int i = 0; i < results.size(); i++) {
            VetQAResult qa = results.get(i);
            context.append(String.format("[%d] %s\n", i + 1, qa.toContext()));
            context.append("\n---\n\n");
        }

        return context.toString();
    }

    /**
     * 지식 벡터 저장 (데이터 로딩 시 사용)
     */
    public void saveKnowledgeVector(VetKnowledge knowledge) {
        try {
            // 질문에 대한 임베딩 생성
            float[] embedding = generateEmbedding(knowledge.getQuestion());

            // Milvus에 저장
            List<Long> knowledgeIds = Collections.singletonList(knowledge.getId());
            List<String> departments = Collections.singletonList(knowledge.getDepartment());
            List<String> contents = Collections.singletonList(
                    truncate(knowledge.getQuestion(), 65000));
            List<List<Float>> embeddings = Collections.singletonList(toList(embedding));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(Arrays.asList(
                            new InsertParam.Field("knowledge_id", knowledgeIds),
                            new InsertParam.Field("department", departments),
                            new InsertParam.Field("content", contents),
                            new InsertParam.Field("embedding", embeddings)))
                    .build();

            milvusClient.insert(insertParam);
            log.debug("💾 벡터 저장: knowledge_id={}", knowledge.getId());

        } catch (Exception e) {
            log.error("❌ 벡터 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 배치 벡터 저장
     */
    public void saveKnowledgeVectorsBatch(List<VetKnowledge> knowledgeList) {
        if (knowledgeList.isEmpty())
            return;

        log.info("💾 배치 벡터 저장 시작: {}개", knowledgeList.size());

        try {
            List<Long> knowledgeIds = new ArrayList<>();
            List<String> departments = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();

            for (VetKnowledge k : knowledgeList) {
                knowledgeIds.add(k.getId());
                departments.add(k.getDepartment());
                contents.add(truncate(k.getQuestion(), 65000));

                float[] embedding = generateEmbedding(k.getQuestion());
                embeddings.add(toList(embedding));
            }

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(Arrays.asList(
                            new InsertParam.Field("knowledge_id", knowledgeIds),
                            new InsertParam.Field("department", departments),
                            new InsertParam.Field("content", contents),
                            new InsertParam.Field("embedding", embeddings)))
                    .build();

            milvusClient.insert(insertParam);
            log.info("✅ 배치 벡터 저장 완료: {}개", knowledgeList.size());

        } catch (Exception e) {
            log.error("❌ 배치 벡터 저장 실패: {}", e.getMessage());
        }
    }

    // ===== Private Methods =====

    private float[] generateEmbedding(String text) {
        float[] embedding = embeddingModel.embed(text);
        return embedding;
    }

    private List<SearchResultItem> searchMilvus(float[] queryEmbedding, String department, int topK) {
        // 필터 표현식 생성
        String filter = department != null ? String.format("department == \"%s\"", department) : "";

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(Collections.singletonList(toList(queryEmbedding)))
                .withVectorFieldName("embedding")
                .withOutFields(Arrays.asList("knowledge_id", "department", "content"))
                .withExpr(filter.isEmpty() ? null : filter)
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);

        List<SearchResultItem> results = new ArrayList<>();

        if (response.getStatus() == R.Status.Success.getCode()) {
            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

            for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
                SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
                float score = idScore.getScore();

                if (score >= MIN_SIMILARITY_SCORE) {
                    Long knowledgeId = (Long) wrapper.getRowRecords(0).get(i).get("knowledge_id");
                    results.add(new SearchResultItem(knowledgeId, score));
                }
            }
        }

        return results;
    }

    private List<Float> toList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * 검색 결과 아이템
     */
    private static class SearchResultItem {
        final Long knowledgeId;
        final float score;

        SearchResultItem(Long knowledgeId, float score) {
            this.knowledgeId = knowledgeId;
            this.score = score;
        }
    }
}
