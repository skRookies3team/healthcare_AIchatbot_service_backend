package com.petlog.healthcare.controller;

import com.petlog.healthcare.dto.hospital.HospitalResponse;
import com.petlog.healthcare.dto.hospital.HospitalResponse.HospitalInfo;
import com.petlog.healthcare.service.HospitalDataLoader;
import com.petlog.healthcare.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 동물병원 검색 API
 *
 * - 현재 위치 기반 주변 병원 검색
 * - 질병/증상별 전문 병원 추천
 * - 지역명 검색
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/api/hospital")
@RequiredArgsConstructor
@Tag(name = "Hospital Search", description = "동물병원 검색 API")
public class HospitalController {

    private final HospitalService hospitalService;
    private final HospitalDataLoader hospitalDataLoader;

    /**
     * 현재 위치 기반 주변 동물병원 검색
     *
     * @param lat    위도 (현재 위치)
     * @param lng    경도 (현재 위치)
     * @param radius 반경 (km, 기본값 5km)
     */
    @GetMapping("/nearby")
    @Operation(summary = "주변 병원 검색", description = "현재 위치 기준 반경 내 동물병원을 거리순으로 검색")
    public ResponseEntity<HospitalResponse> findNearby(
            @Parameter(description = "위도 (예: 37.5012)") @RequestParam double lat,
            @Parameter(description = "경도 (예: 127.0396)") @RequestParam double lng,
            @Parameter(description = "반경 km (기본: 5)") @RequestParam(defaultValue = "5") double radius) {

        log.info("═══════════════════════════════════════");
        log.info("🏥 주변 병원 검색");
        log.info("   위치: ({}, {}), 반경: {}km", lat, lng, radius);
        log.info("═══════════════════════════════════════");

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader.findNearby(lat, lng, radius);
            log.info("✅ 검색 결과: {}개", hospitals.size());
            return ResponseEntity.ok(HospitalResponse.success(hospitals));
        } catch (Exception e) {
            log.error("❌ 검색 실패: {}", e.getMessage());
            return ResponseEntity.ok(HospitalResponse.error("검색 중 오류: " + e.getMessage()));
        }
    }

    /**
     * 질병/증상 기반 전문 병원 추천
     *
     * @param disease 질병/증상 (예: 피부, 알러지, 관절)
     */
    @GetMapping("/specialty")
    @Operation(summary = "전문 병원 검색", description = "질병/증상에 전문적인 동물병원 검색")
    public ResponseEntity<HospitalResponse> findBySpecialty(
            @Parameter(description = "질병/증상 키워드 (예: 피부, 알러지, 안과)") @RequestParam String disease) {

        log.info("🔬 전문 병원 검색 - 질병: {}", disease);

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader.findBySpecialty(disease);
            log.info("✅ 전문 병원: {}개", hospitals.size());
            return ResponseEntity.ok(HospitalResponse.success(hospitals));
        } catch (Exception e) {
            log.error("❌ 검색 실패: {}", e.getMessage());
            return ResponseEntity.ok(HospitalResponse.error("검색 중 오류: " + e.getMessage()));
        }
    }

    /**
     * 위치 + 질병 기반 통합 추천
     *
     * 피부질환 분석 후 근처 피부 전문 병원 추천에 사용
     *
     * @param lat     위도
     * @param lng     경도
     * @param radius  반경 (km)
     * @param disease 질병/증상
     */
    @GetMapping("/recommend")
    @Operation(summary = "병원 추천", description = "위치와 질병을 고려한 맞춤 병원 추천")
    public ResponseEntity<HospitalResponse> recommend(
            @Parameter(description = "위도") @RequestParam double lat,
            @Parameter(description = "경도") @RequestParam double lng,
            @Parameter(description = "반경 km") @RequestParam(defaultValue = "10") double radius,
            @Parameter(description = "질병/증상") @RequestParam(required = false) String disease) {

        log.info("═══════════════════════════════════════");
        log.info("🎯 맞춤 병원 추천");
        log.info("   위치: ({}, {}), 반경: {}km", lat, lng, radius);
        log.info("   질병: {}", disease != null ? disease : "전체");
        log.info("═══════════════════════════════════════");

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader
                    .findNearbyBySpecialty(lat, lng, radius, disease);

            log.info("✅ 추천 병원: {}개", hospitals.size());
            return ResponseEntity.ok(HospitalResponse.success(hospitals));
        } catch (Exception e) {
            log.error("❌ 추천 실패: {}", e.getMessage());
            return ResponseEntity.ok(HospitalResponse.error("추천 중 오류: " + e.getMessage()));
        }
    }

    /**
     * 지역별 동물병원 검색
     */
    @GetMapping("/search")
    @Operation(summary = "지역별 검색", description = "지역명으로 동물병원 검색")
    public ResponseEntity<HospitalResponse> search(
            @Parameter(description = "지역명 (예: 강남, 서울)") @RequestParam String region) {

        log.info("🗺️ 지역별 검색 - region: {}", region);
        return ResponseEntity.ok(hospitalService.findByRegion(region));
    }

    /**
     * 24시/응급 병원 검색
     */
    @GetMapping("/emergency")
    @Operation(summary = "응급 병원 검색", description = "24시간/응급 동물병원 검색")
    public ResponseEntity<HospitalResponse> findEmergency() {
        log.info("🚨 응급 병원 검색");
        return ResponseEntity.ok(hospitalService.findEmergencyHospitals());
    }

    /**
     * 병원 데이터 통계
     */
    @GetMapping("/stats")
    @Operation(summary = "병원 통계", description = "로드된 병원 데이터 통계")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(java.util.Map.of(
                "totalHospitals", hospitalDataLoader.getTotalCount(),
                "emergencyHospitals", hospitalDataLoader.findEmergencyHospitals().size(),
                "status", "OK"));
    }

    /**
     * API 상태 확인
     */
    @GetMapping("/health")
    @Operation(summary = "API 상태 확인")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Hospital API is UP - " + hospitalDataLoader.getTotalCount() + " hospitals loaded");
    }
}
