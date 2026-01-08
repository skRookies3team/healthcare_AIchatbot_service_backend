package com.petlog.healthcare.dto.hospital;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 동물병원 검색 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalResponse {

    private boolean success;
    private String message;
    private int totalCount;
    private List<HospitalInfo> hospitals;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalInfo {
        private String name; // 병원명
        private String address; // 주소
        private String roadAddress; // 도로명 주소
        private String phone; // 전화번호
        private Double latitude; // 위도
        private Double longitude; // 경도
        private Double distance; // 거리 (km)
        private String operatingHours; // 운영시간
        private boolean isEmergency; // 24시간/응급 여부
        private String specialty; // 전문 진료 (피부과, 안과 등)
    }

    public static HospitalResponse success(List<HospitalInfo> hospitals) {
        return HospitalResponse.builder()
                .success(true)
                .message("검색 완료")
                .totalCount(hospitals.size())
                .hospitals(hospitals)
                .build();
    }

    public static HospitalResponse error(String message) {
        return HospitalResponse.builder()
                .success(false)
                .message(message)
                .totalCount(0)
                .hospitals(List.of())
                .build();
    }
}
