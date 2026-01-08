package com.petlog.healthcare.service;

import com.petlog.healthcare.dto.hospital.HospitalResponse;
import com.petlog.healthcare.dto.hospital.HospitalResponse.HospitalInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 동물병원 검색 서비스
 *
 * CSV 파일에서 로드한 병원 데이터 검색
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalDataLoader hospitalDataLoader;

    /**
     * 주변 동물병원 검색 (위치 기반 - 현재는 전체 반환)
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radiusKm  반경 (km)
     * @return 병원 목록
     */
    public HospitalResponse findNearbyHospitals(double latitude, double longitude, int radiusKm) {
        log.info("🏥 주변 동물병원 검색");
        log.info("   위치: ({}, {}), 반경: {}km", latitude, longitude, radiusKm);

        try {
            // 현재는 전체 데이터에서 상위 20개 반환
            // TODO: 좌표 기반 거리 계산 추가
            List<HospitalInfo> hospitals = hospitalDataLoader.getAllHospitals()
                    .stream()
                    .limit(20)
                    .toList();

            log.info("✅ 검색 결과: {}개 병원", hospitals.size());
            return HospitalResponse.success(hospitals);

        } catch (Exception e) {
            log.error("❌ 병원 검색 실패: {}", e.getMessage(), e);
            return HospitalResponse.error("검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 지역명으로 동물병원 검색
     *
     * @param region 지역명 (예: 서울, 강남구)
     * @return 병원 목록
     */
    public HospitalResponse findByRegion(String region) {
        log.info("🏥 지역별 동물병원 검색 - region: {}", region);

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader.findByRegion(region);

            log.info("✅ 검색 결과: {}개 병원 (지역: {})", hospitals.size(), region);
            return HospitalResponse.success(hospitals);

        } catch (Exception e) {
            log.error("❌ 병원 검색 실패: {}", e.getMessage(), e);
            return HospitalResponse.error("검색 중 오류가 발생했습니다.");
        }
    }

    /**
     * 병원명으로 검색
     *
     * @param keyword 검색어
     * @return 병원 목록
     */
    public HospitalResponse searchByName(String keyword) {
        log.info("🔍 병원명 검색 - keyword: {}", keyword);

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader.searchByName(keyword);

            log.info("✅ 검색 결과: {}개 병원", hospitals.size());
            return HospitalResponse.success(hospitals);

        } catch (Exception e) {
            log.error("❌ 검색 실패: {}", e.getMessage(), e);
            return HospitalResponse.error("검색 중 오류가 발생했습니다.");
        }
    }

    /**
     * 24시/응급 병원 검색
     *
     * @return 응급 병원 목록
     */
    public HospitalResponse findEmergencyHospitals() {
        log.info("🚨 응급 동물병원 검색");

        try {
            List<HospitalInfo> hospitals = hospitalDataLoader.findEmergencyHospitals();

            log.info("✅ 응급 병원: {}개", hospitals.size());
            return HospitalResponse.success(hospitals);

        } catch (Exception e) {
            log.error("❌ 검색 실패: {}", e.getMessage(), e);
            return HospitalResponse.error("검색 중 오류가 발생했습니다.");
        }
    }

    /**
     * 전체 병원 수 조회
     */
    public int getTotalCount() {
        return hospitalDataLoader.getTotalCount();
    }
}
