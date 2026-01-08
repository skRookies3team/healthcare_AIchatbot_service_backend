package com.petlog.healthcare.dto.withapet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WithaPet 스마트 청진기 건강 데이터 DTO
 * WHY: 실제 WITHAPET 기기 연동 전 목업 데이터 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithaPetHealthData {

    // ========== 펫 프로필 ==========
    private String petName; // 펫 이름 (사용자 등록명으로 치환)
    private String species; // 종류 (Dog/Cat)
    private String age; // 나이 (예: "1년 6개월")
    private String breed; // 품종
    private String sex; // 성별 (Male/Female)

    // ========== 오늘의 건강 점수 ==========
    private Integer healthScore; // 건강 점수 (0-100)
    private String healthStatus; // 건강 상태 (매우 좋음/좋음/보통/주의/위험)
    private List<String> healthTags; // 상태 태그 (예: ["심박수 정상", "활동량 우수"])

    // ========== 바이탈 데이터 ==========
    private VitalData vitalData;

    // ========== AI 분석 결과 ==========
    private AIAnalysis aiAnalysis;

    // ========== 바이탈 트렌드 (24시간 모니터링) ==========
    private List<VitalTrendPoint> heartRateTrend; // 심박수 트렌드
    private List<VitalTrendPoint> respiratoryRateTrend; // 호흡수 트렌드

    // ========== 메타데이터 ==========
    private LocalDateTime measurementDate; // 측정일시
    private LocalDateTime issuedDate; // 발급일
    private String dataSource; // 데이터 출처 (mock/withapet)

    /**
     * 바이탈 데이터 (평균 심박수, 호흡수, 체중, 컨디션)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalData {
        private Integer avgHeartRate; // 평균 심박수 (BPM)
        private String heartRateStatus; // 상태 (정상 범위/빠름/느림)

        private Integer avgRespiratoryRate; // 분당 호흡수 (RPM)
        private String respiratoryStatus; // 상태 (안정적/불안정)

        private Double weight; // 몸무게 (kg)
        private String weightStatus; // 상태 (변화 없음/증가/감소)

        private Integer conditionScore; // 컨디션 지수 (0-100)
        private String conditionStatus; // 상태 (최상/양호/보통/주의)
    }

    /**
     * AI 분석 결과 (MMVD 등)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIAnalysis {
        private Integer normalPercent; // 정상 확률 (%)
        private Integer abnormalPercent; // 비정상 확률 (%)
        private String analysisResult; // 분석 결과 (정상/주의요망/이상의심)
        private String mmvdStage; // MMVD 단계 (N/B1/B2/C/D)
        private String recommendation; // 권장 사항
    }

    /**
     * 바이탈 트렌드 포인트 (차트용)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalTrendPoint {
        private String time; // 시간 (예: "06:00", "12:00")
        private Integer value; // 측정값
        private String zone; // 구간 (normal/caution/danger)
    }
}
