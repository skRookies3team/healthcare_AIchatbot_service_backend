package com.petlog.healthcare.controller;

import com.petlog.healthcare.service.SimpleFileRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 테스트 컨트롤러
 *
 * RAG 시스템이 제대로 동작하는지 확인
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final SimpleFileRagService ragService;

    /**
     * RAG 검색 테스트
     *
     * GET /test/rag?query=강아지 방광염
     */
    @GetMapping("/rag")
    public ResponseEntity<Map<String, Object>> testRag(@RequestParam String query) {
        log.info("🧪 RAG 테스트 시작: '{}'", query);

        try {
            String ragResult = ragService.search(query);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("ragContext", ragResult);
            response.put("contextLength", ragResult.length());

            log.info("✅ RAG 테스트 성공");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ RAG 테스트 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 문서 로딩 상태 확인
     *
     * GET /test/rag-status
     */
    @GetMapping("/rag-status")
    public ResponseEntity<Map<String, Object>> testRagStatus() {
        log.info("🔍 RAG 상태 확인");

        try {
            // 간단한 검색으로 상태 확인
            String result = ragService.search("테스트");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "RAG 시스템 정상 작동");
            response.put("testResultLength", result.length());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ RAG 상태 확인 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}