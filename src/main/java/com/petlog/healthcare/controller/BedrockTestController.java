package com.petlog.healthcare.controller;

import com.petlog.healthcare.infrastructure.bedrock.TitanEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Bedrock 테스트 컨트롤러
 * 
 * Titan Embeddings 및 Claude 연결 테스트용
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class BedrockTestController {

    private final TitanEmbeddingClient titanEmbeddingClient;

    /**
     * Titan Embeddings 테스트
     *
     * GET /api/test/embeddings?text=테스트
     */
    @GetMapping("/embeddings")
    public ResponseEntity<Map<String, Object>> testEmbeddings(
            @RequestParam(defaultValue = "반려동물 건강 테스트") String text) {

        log.info("🧪 Titan Embeddings 테스트 시작 - text: {}", text);

        try {
            long startTime = System.currentTimeMillis();
            float[] embedding = titanEmbeddingClient.generateEmbedding(text);
            long elapsed = System.currentTimeMillis() - startTime;

            log.info("✅ Titan Embeddings 성공! 벡터 차원: {}, 소요시간: {}ms",
                    embedding.length, elapsed);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Titan Embeddings 정상 작동!",
                    "inputText", text,
                    "vectorDimension", embedding.length,
                    "elapsedMs", elapsed,
                    "sampleVector", new float[] { embedding[0], embedding[1], embedding[2] }));

        } catch (Exception e) {
            log.error("❌ Titan Embeddings 실패: {}", e.getMessage());

            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage(),
                    "hint", "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_BEDROCK_REGION 확인 필요"));
        }
    }
}
