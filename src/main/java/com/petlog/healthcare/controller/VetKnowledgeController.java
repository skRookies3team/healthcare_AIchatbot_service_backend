package com.petlog.healthcare.controller;

import com.petlog.healthcare.dto.vet.VetQAResult;
import com.petlog.healthcare.service.VetQADataLoader;
import com.petlog.healthcare.service.VetKnowledgeSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 수의사 지식 베이스 API 컨트롤러
 * WHY: 데이터 로딩, 검색, 통계 조회 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/vet/knowledge")
@RequiredArgsConstructor
public class VetKnowledgeController {

    private final VetQADataLoader vetQADataLoader;
    private final VetKnowledgeSearchService vetKnowledgeSearchService;

    /**
     * 📊 데이터 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        String stats = vetQADataLoader.getDataStats();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats));
    }

    /**
     * 📥 데이터 로딩 (JSON → DB)
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadData() {
        log.info("🐕 수의사 Q&A 데이터 로딩 요청");

        int loaded = vetQADataLoader.loadAllData();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "데이터 로딩 완료",
                "loadedCount", loaded));
    }

    /**
     * 🔍 RAG 검색 테스트
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestBody SearchRequest request) {

        log.info("🔍 RAG 검색 요청: query='{}', dept={}", request.query(), request.department());

        List<VetQAResult> results = vetKnowledgeSearchService.searchRelevantQA(
                request.query(),
                request.department(),
                request.topK() != null ? request.topK() : 5);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "query", request.query(),
                "department", request.department() != null ? request.department() : "전체",
                "resultCount", results.size(),
                "results", results));
    }

    /**
     * 📚 RAG 컨텍스트 생성 테스트
     */
    @PostMapping("/context")
    public ResponseEntity<Map<String, Object>> buildContext(
            @RequestBody SearchRequest request) {

        String context = vetKnowledgeSearchService.buildRAGContext(
                request.query(),
                request.department(),
                request.topK() != null ? request.topK() : 3);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "query", request.query(),
                "context", context,
                "contextLength", context.length()));
    }

    /**
     * 🗑️ 데이터 초기화 (개발용)
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearData() {
        log.warn("⚠️ 수의사 Q&A 데이터 삭제 요청");
        vetQADataLoader.clearAllData();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "데이터 삭제 완료"));
    }

    /**
     * 검색 요청 DTO
     */
    public record SearchRequest(
            String query,
            String department,
            Integer topK) {
    }
}
