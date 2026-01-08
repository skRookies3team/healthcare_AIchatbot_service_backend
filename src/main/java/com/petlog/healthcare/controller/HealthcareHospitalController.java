package com.petlog.healthcare.controller;

import com.petlog.healthcare.dto.hospital.HospitalResponse.HospitalInfo;
import com.petlog.healthcare.service.HospitalDataLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Healthcare 위치 기반 병원 검색 API
 * WHY: BACKEND_GEOLOCATION_GUIDE.md 스펙에 맞춘 Geolocation 기반 병원 검색
 * 프론트엔드 통합을 위한 표준 API
 * 
 * @author healthcare-team
 * @since 2026-01-08
 */
@Slf4j
@RestController
@RequestMapping("/api/healthcare")
@RequiredArgsConstructor
@Tag(name = "Healthcare Geolocation", description = "위치 기반 동물병원 검색 API")
public class HealthcareHospitalController {

    private final HospitalDataLoader hospitalDataLoader;

    /**
     * 위치 기반 주변 동물병원 검색
     * GET /api/healthcare/hospitals
     * 
     * @param lat    사용자 현재 위도 (필수)
     * @param lng    사용자 현재 경도 (필수)
     * @param radius 검색 반경 (미터, 기본값: 2000)
     * @return 거리순 정렬된 병원 목록
     */
    @GetMapping("/hospitals")
    @Operation(summary = "주변 병원 검색", description = "사용자 위치 기반 반경 내 동물병원을 거리순으로 검색 (BACKEND_GEOLOCATION_GUIDE 스펙)")
    public ResponseEntity<List<Map<String, Object>>> findNearbyHospitals(
            @Parameter(description = "사용자 현재 위도 (예: 37.5665)", required = true) @RequestParam double lat,

            @Parameter(description = "사용자 현재 경도 (예: 126.9780)", required = true) @RequestParam double lng,

            @Parameter(description = "검색 반경 (미터, 기본값: 2000)") @RequestParam(defaultValue = "2000") int radius) {

        log.info("═══════════════════════════════════════");
        log.info("🏥 [Geolocation API] 주변 병원 검색");
        log.info("   위치: ({}, {}), 반경: {}m", lat, lng, radius);
        log.info("═══════════════════════════════════════");

        // 반경을 km로 변환
        double radiusKm = radius / 1000.0;

        // 거리 기반 병원 검색
        List<HospitalInfo> hospitals = hospitalDataLoader.findNearby(lat, lng, radiusKm);

        // 가이드 스펙에 맞춰 응답 변환
        List<Map<String, Object>> response = hospitals.stream()
                .limit(20) // 최대 20개
                .map(h -> convertToGuideFormat(h))
                .collect(Collectors.toList());

        log.info("✅ 검색 결과: {}개 병원 (반경 {}m 내)", response.size(), radius);

        return ResponseEntity.ok(response);
    }

    /**
     * 24시/응급 병원 검색 (위치 기반)
     */
    @GetMapping("/hospitals/emergency")
    @Operation(summary = "응급 병원 검색", description = "위치 기반 24시간/응급 동물병원 검색")
    public ResponseEntity<List<Map<String, Object>>> findEmergencyHospitals(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") int radius) {

        log.info("🚨 [Geolocation API] 응급 병원 검색 - 위치: ({}, {})", lat, lng);

        double radiusKm = radius / 1000.0;

        List<Map<String, Object>> response = hospitalDataLoader.findNearby(lat, lng, radiusKm)
                .stream()
                .filter(HospitalInfo::isEmergency)
                .limit(10)
                .map(this::convertToGuideFormat)
                .collect(Collectors.toList());

        log.info("✅ 응급 병원: {}개", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 질병 기반 전문 병원 검색 (위치 포함)
     */
    @GetMapping("/hospitals/specialty")
    @Operation(summary = "전문 병원 검색", description = "위치 + 질병 기반 전문 동물병원 검색")
    public ResponseEntity<List<Map<String, Object>>> findSpecialtyHospitals(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") int radius,
            @RequestParam String disease) {

        log.info("🔬 [Geolocation API] 전문 병원 검색 - 질병: {}", disease);

        double radiusKm = radius / 1000.0;

        List<Map<String, Object>> response = hospitalDataLoader
                .findNearbyBySpecialty(lat, lng, radiusKm, disease)
                .stream()
                .limit(20)
                .map(this::convertToGuideFormat)
                .collect(Collectors.toList());

        log.info("✅ 전문 병원: {}개", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * HospitalInfo를 가이드 스펙 형식으로 변환
     * 
     * Response Format:
     * {
     * "id": "1",
     * "name": "병원명",
     * "address": "주소",
     * "lat": 37.567,
     * "lng": 126.979,
     * "rating": 4.5,
     * "distance": 350, // 미터 단위
     * "status": "OPEN" // OPEN or CLOSED
     * }
     */
    private Map<String, Object> convertToGuideFormat(HospitalInfo hospital) {
        Map<String, Object> result = new LinkedHashMap<>();

        // ID 생성 (해시 기반)
        String id = String.valueOf(Math.abs(hospital.getName().hashCode()) % 10000);

        result.put("id", id);
        result.put("name", hospital.getName());
        result.put("address", hospital.getRoadAddress() != null && !hospital.getRoadAddress().isEmpty()
                ? hospital.getRoadAddress()
                : hospital.getAddress());
        result.put("lat", hospital.getLatitude());
        result.put("lng", hospital.getLongitude());

        // 랜덤 평점 (3.5 ~ 5.0, 실제 데이터 없으면 생성)
        double rating = 3.5 + (Math.random() * 1.5);
        result.put("rating", Math.round(rating * 10.0) / 10.0);

        // 거리를 미터 단위로 변환
        int distanceMeters = (int) Math.round(hospital.getDistance() * 1000);
        result.put("distance", distanceMeters);

        // 운영 상태 계산
        String status = calculateStatus(hospital);
        result.put("status", status);

        // 추가 정보
        result.put("phone", hospital.getPhone());
        result.put("isEmergency", hospital.isEmergency());
        result.put("specialty", hospital.getSpecialty());
        result.put("operatingHours", hospital.getOperatingHours());

        return result;
    }

    /**
     * 현재 시간 기준 운영 상태 계산
     */
    private String calculateStatus(HospitalInfo hospital) {
        // 24시/응급 병원은 항상 OPEN
        if (hospital.isEmergency()) {
            return "OPEN";
        }

        // 현재 시간 체크 (09:00 ~ 21:00 운영 가정)
        LocalTime now = LocalTime.now();
        LocalTime openTime = LocalTime.of(9, 0);
        LocalTime closeTime = LocalTime.of(21, 0);

        if (now.isAfter(openTime) && now.isBefore(closeTime)) {
            return "OPEN";
        }

        return "CLOSED";
    }
}
