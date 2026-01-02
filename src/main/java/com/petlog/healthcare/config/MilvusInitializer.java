package com.petlog.healthcare.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Milvus 초기화 (Titan Embeddings 1024차원)
 *
 * WHY?
 * - Diary Service는 OpenAI (1536차원)
 * - Healthcare Service는 Titan (1024차원) 사용
 * - 각 서비스마다 별도 컬렉션 필요
 *
 * @author healthcare-team
 * @since 2026-01-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusInitializer {

    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collection-name:diary_vectors}")
    private String collectionName;

    @Value("${milvus.embedding-dimension:1024}")
    private int embeddingDimension;

    @PostConstruct
    public void initialize() {
        try {
            log.info("═══════════════════════════════════════");
            log.info("🚀 Milvus 초기화 시작");
            log.info("   Collection: {}", collectionName);
            log.info("   Dimension: {} (Titan Embeddings)", embeddingDimension);
            log.info("═══════════════════════════════════════");

            // 1. 컬렉션 존재 여부 확인
            if (hasCollection()) {
                log.info("✅ 컬렉션이 이미 존재합니다: {}", collectionName);
                loadCollection();
                return;
            }

            // 2. 컬렉션 생성
            createCollection();

            // 3. 인덱스 생성
            createIndex();

            // 4. 컬렉션 로드
            loadCollection();

            log.info("═══════════════════════════════════════");
            log.info("✅ Milvus 초기화 완료!");
            log.info("═══════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ Milvus 초기화 실패", e);
            throw new RuntimeException("Milvus 초기화 실패", e);
        }
    }

    private boolean hasCollection() {
        HasCollectionParam param = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        return milvusClient.hasCollection(param).getData();
    }

    private void createCollection() {
        log.info("📝 컬렉션 생성 중...");

        // 필드 정의
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType diaryIdField = FieldType.newBuilder()
                .withName("diary_id")
                .withDataType(DataType.Int64)
                .build();

        FieldType userIdField = FieldType.newBuilder()
                .withName("user_id")
                .withDataType(DataType.Int64)
                .build();

        FieldType petIdField = FieldType.newBuilder()
                .withName("pet_id")
                .withDataType(DataType.Int64)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType embeddingField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(embeddingDimension) // 1024차원
                .build();

        // 컬렉션 생성
        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Healthcare Service - Diary Vectors (Titan 1024D)")
                .withFieldTypes(Arrays.asList(
                        idField, diaryIdField, userIdField, petIdField,
                        contentField, embeddingField
                ))
                .build();

        milvusClient.createCollection(param);
        log.info("✅ 컬렉션 생성 완료");
    }

    private void createIndex() {
        log.info("🔍 인덱스 생성 중...");

        CreateIndexParam param = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"nlist\":128}")
                .withSyncMode(Boolean.TRUE)
                .build();

        milvusClient.createIndex(param);
        log.info("✅ 인덱스 생성 완료");
    }

    private void loadCollection() {
        log.info("💾 컬렉션 로드 중...");

        LoadCollectionParam param = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        milvusClient.loadCollection(param);
        log.info("✅ 컬렉션 로드 완료");
    }
}