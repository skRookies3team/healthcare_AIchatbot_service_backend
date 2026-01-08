package com.petlog.healthcare.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health Check Controller
 *
 * Gateway에서 서비스 상태를 확인하는 엔드포인트
 * Public endpoint (JWT 불필요)
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${server.port:8085}")
    private String port;

    @Value("${spring.application.name:healthcare-service}")
    private String serviceName;

    /**
     * 기본 Health Check
     * GET /api/health
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", serviceName);
        status.put("port", port);
        status.put("timestamp", LocalDateTime.now().toString());

        log.debug("Health check requested");
        return ResponseEntity.ok(status);
    }

    /**
     * 상세 Health Check
     * GET /api/health/detail
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> healthDetail() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", serviceName);
        status.put("port", port);
        status.put("components", Map.of(
                "bedrock", "Claude Haiku 4.5 (ap-northeast-2)",
                "kafka", "healthcare-group",
                "milvus", "diary_vectors collection"));
        status.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(status);
    }
}