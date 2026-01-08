package com.petlog.healthcare.service;

import com.petlog.healthcare.dto.hospital.HospitalResponse.HospitalInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 동물병원 CSV 데이터 로더
 *
 * 공공데이터포털에서 받은 CSV 파일을 로드하여 메모리에 캐싱
 * 위치 기반 거리 계산 지원
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Service
public class HospitalDataLoader {

    private final List<HospitalInfo> allHospitals = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadHospitalData();
    }

    /**
     * CSV 파일에서 병원 데이터 로드
     */
    private void loadHospitalData() {
        log.info("═══════════════════════════════════════");
        log.info("🏥 동물병원 CSV 데이터 로딩 시작");

        try {
            // 여러 경로 시도
            Resource resource = null;
            String[] paths = {
                    "data/hospital_data.csv",
                    "동물병원_DATA.csv",
                    "hospital_data.csv"
            };

            for (String path : paths) {
                Resource r = new ClassPathResource(path);
                if (r.exists()) {
                    resource = r;
                    log.info("✅ CSV 파일 발견: {}", path);
                    break;
                }
            }

            if (resource == null) {
                log.warn("⚠️ CSV 파일 없음 - 샘플 데이터 사용");
                loadSampleData();
                return;
            }

            // EUC-KR 또는 UTF-8 시도
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), Charset.forName("EUC-KR")))) {
                parseCSV(reader);
            } catch (Exception e) {
                log.info("EUC-KR 실패, UTF-8로 재시도");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), Charset.forName("UTF-8")))) {
                    parseCSV(reader);
                }
            }

        } catch (Exception e) {
            log.error("❌ CSV 로드 실패: {}", e.getMessage());
            loadSampleData();
        }

        log.info("═══════════════════════════════════════");
    }

    private void parseCSV(BufferedReader reader) throws Exception {
        // 헤더 읽기
        String header = reader.readLine();
        log.info("📋 CSV 헤더: {}", header);

        // 데이터 파싱
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            try {
                HospitalInfo hospital = parseCsvLine(line, header);
                if (hospital != null) {
                    allHospitals.add(hospital);
                    count++;
                }
            } catch (Exception e) {
                log.debug("CSV 라인 파싱 오류: {}", e.getMessage());
            }
        }

        log.info("✅ 병원 데이터 로드 완료: {}개", count);
    }

    /**
     * CSV 라인 파싱 (다양한 컬럼 구조 지원)
     */
    private HospitalInfo parseCsvLine(String line, String header) {
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String[] headers = header.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 2)
            return null;

        String name = "";
        String address = "";
        String roadAddress = "";
        String phone = "";
        double lat = 0.0;
        double lng = 0.0;
        String specialty = "";

        // 헤더 기반 파싱
        for (int i = 0; i < Math.min(headers.length, parts.length); i++) {
            String h = cleanValue(headers[i]).toLowerCase();
            String v = cleanValue(parts[i]);

            if (h.contains("사업장명") || h.contains("업소명") || h.contains("병원명") || h.contains("name")) {
                name = v;
            } else if (h.contains("소재지전체") || h.contains("주소") && address.isEmpty()) {
                address = v;
            } else if (h.contains("도로명") || h.contains("road")) {
                roadAddress = v;
            } else if (h.contains("전화") || h.contains("phone") || h.contains("연락처")) {
                phone = v;
            } else if (h.contains("위도") || h.contains("lat")) {
                lat = parseDouble(v);
            } else if (h.contains("경도") || h.contains("lng") || h.contains("lon")) {
                lng = parseDouble(v);
            } else if (h.contains("전문") || h.contains("specialty") || h.contains("진료과목")) {
                specialty = v;
            }
        }

        // 이름 없으면 첫 번째 컬럼 사용
        if (name.isEmpty() && parts.length > 0) {
            name = cleanValue(parts[0]);
        }
        if (address.isEmpty() && parts.length > 1) {
            address = cleanValue(parts[1]);
        }
        if (phone.isEmpty() && parts.length > 2) {
            phone = cleanValue(parts[2]);
        }

        if (name.isEmpty() || name.equals("사업장명"))
            return null;

        return HospitalInfo.builder()
                .name(name)
                .address(address)
                .roadAddress(roadAddress.isEmpty() ? address : roadAddress)
                .phone(phone)
                .latitude(lat)
                .longitude(lng)
                .distance(0.0)
                .operatingHours("운영시간 문의")
                .isEmergency(name.contains("24시") || name.contains("응급"))
                .specialty(specialty)
                .build();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String cleanValue(String value) {
        if (value == null)
            return "";
        return value.replace("\"", "").trim();
    }

    /**
     * 샘플 데이터 로드 (CSV 없을 때)
     */
    private void loadSampleData() {
        log.info("📦 샘플 데이터 로드");

        allHospitals.add(HospitalInfo.builder()
                .name("24시 미래동물병원")
                .address("서울특별시 강남구 역삼동 123-45")
                .roadAddress("서울특별시 강남구 테헤란로 123")
                .phone("02-1234-5678")
                .latitude(37.5012)
                .longitude(127.0396)
                .distance(0.0)
                .operatingHours("24시간")
                .isEmergency(true)
                .specialty("응급진료,피부과")
                .build());

        allHospitals.add(HospitalInfo.builder()
                .name("서울펫동물병원")
                .address("서울특별시 강남구 삼성동 234-56")
                .roadAddress("서울특별시 강남구 삼성로 456")
                .phone("02-2345-6789")
                .latitude(37.5112)
                .longitude(127.0596)
                .distance(0.0)
                .operatingHours("09:00 - 21:00")
                .isEmergency(false)
                .specialty("피부과,안과")
                .build());

        allHospitals.add(HospitalInfo.builder()
                .name("강남피부클리닉동물병원")
                .address("서울특별시 강남구 논현동 345-67")
                .roadAddress("서울특별시 강남구 논현로 789")
                .phone("02-3456-7890")
                .latitude(37.5150)
                .longitude(127.0300)
                .distance(0.0)
                .operatingHours("10:00 - 20:00")
                .isEmergency(false)
                .specialty("피부과,알러지")
                .build());

        log.info("📦 샘플 데이터 {}개 로드", allHospitals.size());
    }

    /**
     * 현재 위치 기반 가까운 병원 찾기 (거리 계산)
     *
     * @param latitude  현재 위도
     * @param longitude 현재 경도
     * @param radiusKm  반경 (km)
     * @return 거리순 정렬된 병원 목록
     */
    public List<HospitalInfo> findNearby(double latitude, double longitude, double radiusKm) {
        return allHospitals.stream()
                .filter(h -> h.getLatitude() != 0 && h.getLongitude() != 0)
                .map(h -> {
                    double dist = calculateDistance(latitude, longitude, h.getLatitude(), h.getLongitude());
                    return HospitalInfo.builder()
                            .name(h.getName())
                            .address(h.getAddress())
                            .roadAddress(h.getRoadAddress())
                            .phone(h.getPhone())
                            .latitude(h.getLatitude())
                            .longitude(h.getLongitude())
                            .distance(Math.round(dist * 100.0) / 100.0)
                            .operatingHours(h.getOperatingHours())
                            .isEmergency(h.isEmergency())
                            .specialty(h.getSpecialty())
                            .build();
                })
                .filter(h -> h.getDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(HospitalInfo::getDistance))
                .collect(Collectors.toList());
    }

    /**
     * 질병/증상 관련 전문 병원 찾기
     *
     * @param disease 질병/증상 키워드 (예: 피부, 알러지, 관절)
     * @return 전문 병원 목록
     */
    public List<HospitalInfo> findBySpecialty(String disease) {
        String keyword = disease.toLowerCase();

        return allHospitals.stream()
                .filter(h -> {
                    String specialty = h.getSpecialty() != null ? h.getSpecialty().toLowerCase() : "";
                    String name = h.getName().toLowerCase();
                    return specialty.contains(keyword) || name.contains(keyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 위치 + 질병 기반 병원 추천
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radiusKm  반경
     * @param disease   질병/증상
     * @return 거리순 정렬된 전문 병원
     */
    public List<HospitalInfo> findNearbyBySpecialty(double latitude, double longitude,
            double radiusKm, String disease) {
        List<HospitalInfo> nearby = findNearby(latitude, longitude, radiusKm);
        String keyword = disease != null ? disease.toLowerCase() : "";

        if (keyword.isEmpty()) {
            return nearby;
        }

        // 전문 병원 우선 + 거리순
        return nearby.stream()
                .sorted((a, b) -> {
                    boolean aMatch = matchesSpecialty(a, keyword);
                    boolean bMatch = matchesSpecialty(b, keyword);
                    if (aMatch && !bMatch)
                        return -1;
                    if (!aMatch && bMatch)
                        return 1;
                    return Double.compare(a.getDistance(), b.getDistance());
                })
                .collect(Collectors.toList());
    }

    private boolean matchesSpecialty(HospitalInfo h, String keyword) {
        String specialty = h.getSpecialty() != null ? h.getSpecialty().toLowerCase() : "";
        String name = h.getName().toLowerCase();
        return specialty.contains(keyword) || name.contains(keyword);
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반경 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // 기존 메서드들
    public List<HospitalInfo> getAllHospitals() {
        return new ArrayList<>(allHospitals);
    }

    public List<HospitalInfo> findByRegion(String region) {
        return allHospitals.stream()
                .filter(h -> h.getAddress().contains(region) ||
                        h.getRoadAddress().contains(region))
                .collect(Collectors.toList());
    }

    public List<HospitalInfo> searchByName(String keyword) {
        return allHospitals.stream()
                .filter(h -> h.getName().contains(keyword))
                .collect(Collectors.toList());
    }

    public List<HospitalInfo> findEmergencyHospitals() {
        return allHospitals.stream()
                .filter(HospitalInfo::isEmergency)
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return allHospitals.size();
    }
}
