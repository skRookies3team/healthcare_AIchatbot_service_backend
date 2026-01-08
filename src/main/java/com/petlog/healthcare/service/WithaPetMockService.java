package com.petlog.healthcare.service;

import com.petlog.healthcare.dto.withapet.WithaPetHealthData;
import com.petlog.healthcare.dto.withapet.WithaPetHealthData.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WithaPet 스마트 청진기 목업 서비스
 * WHY: 실제 WITHAPET 기기 연동 전 목업 데이터 제공
 * 33개 PDF 리포트 데이터 기반 샘플 데이터셋 사용
 */
@Slf4j
@Service
public class WithaPetMockService {

    // 샘플 데이터셋 (33개 PDF 기반)
    private final List<SampleData> sampleDataset;

    public WithaPetMockService() {
        this.sampleDataset = initializeSampleDataset();
        log.info("[WithaPet Mock] Initialized with {} sample datasets", sampleDataset.size());
    }

    /**
     * 목업 건강 데이터 조회
     * 사용자 펫 이름으로 치환하여 반환
     */
    public WithaPetHealthData getMockHealthData(String petName, String petType) {
        // 랜덤 샘플 선택
        SampleData sample = getRandomSample();

        // 현재 시간 기준 트렌드 데이터 생성
        List<VitalTrendPoint> heartRateTrend = generateHeartRateTrend(sample.avgHeartRate);
        List<VitalTrendPoint> respiratoryTrend = generateRespiratoryTrend(sample.avgRespiratoryRate);

        // 건강 점수 계산
        int healthScore = calculateHealthScore(sample);
        String healthStatus = getHealthStatus(healthScore);
        List<String> healthTags = generateHealthTags(sample);

        return WithaPetHealthData.builder()
                // 펫 프로필 (사용자 입력으로 치환)
                .petName(petName)
                .species(petType != null ? petType : sample.species)
                .age(sample.age)
                .breed(sample.breed)
                .sex(sample.sex)

                // 건강 점수
                .healthScore(healthScore)
                .healthStatus(healthStatus)
                .healthTags(healthTags)

                // 바이탈 데이터
                .vitalData(VitalData.builder()
                        .avgHeartRate(sample.avgHeartRate)
                        .heartRateStatus(getHeartRateStatus(sample.avgHeartRate, petType))
                        .avgRespiratoryRate(sample.avgRespiratoryRate)
                        .respiratoryStatus(getRespiratoryStatus(sample.avgRespiratoryRate))
                        .weight(sample.weight)
                        .weightStatus("변화 없음")
                        .conditionScore(healthScore)
                        .conditionStatus(getConditionStatus(healthScore))
                        .build())

                // AI 분석
                .aiAnalysis(AIAnalysis.builder()
                        .normalPercent(sample.normalPercent)
                        .abnormalPercent(100 - sample.normalPercent)
                        .analysisResult(sample.analysisResult)
                        .mmvdStage(sample.mmvdStage)
                        .recommendation(getRecommendation(sample.analysisResult))
                        .build())

                // 트렌드 데이터
                .heartRateTrend(heartRateTrend)
                .respiratoryRateTrend(respiratoryTrend)

                // 메타데이터
                .measurementDate(LocalDateTime.now().minusHours(ThreadLocalRandom.current().nextInt(1, 24)))
                .issuedDate(LocalDateTime.now())
                .dataSource("mock")
                .build();
    }

    /**
     * 샘플 데이터 목록 조회
     */
    public List<Map<String, Object>> getSampleList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < sampleDataset.size(); i++) {
            SampleData s = sampleDataset.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("species", s.species);
            item.put("heartRate", s.avgHeartRate);
            item.put("analysisResult", s.analysisResult);
            item.put("mmvdStage", s.mmvdStage);
            list.add(item);
        }
        return list;
    }

    // ========== Private Methods ==========

    private SampleData getRandomSample() {
        int index = ThreadLocalRandom.current().nextInt(sampleDataset.size());
        return sampleDataset.get(index);
    }

    private List<VitalTrendPoint> generateHeartRateTrend(int baseRate) {
        List<VitalTrendPoint> trend = new ArrayList<>();
        String[] times = { "00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "현재" };

        for (String time : times) {
            int variation = ThreadLocalRandom.current().nextInt(-15, 16);
            int value = baseRate + variation;
            String zone = getHeartRateZone(value);
            trend.add(VitalTrendPoint.builder()
                    .time(time)
                    .value(value)
                    .zone(zone)
                    .build());
        }
        return trend;
    }

    private List<VitalTrendPoint> generateRespiratoryTrend(int baseRate) {
        List<VitalTrendPoint> trend = new ArrayList<>();
        String[] times = { "00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "현재" };

        for (String time : times) {
            int variation = ThreadLocalRandom.current().nextInt(-5, 6);
            int value = Math.max(10, baseRate + variation);
            String zone = getRespiratoryZone(value);
            trend.add(VitalTrendPoint.builder()
                    .time(time)
                    .value(value)
                    .zone(zone)
                    .build());
        }
        return trend;
    }

    private int calculateHealthScore(SampleData sample) {
        // 기본 점수: AI 정상 확률 기반
        int baseScore = sample.normalPercent;

        // MMVD 단계에 따른 조정
        switch (sample.mmvdStage) {
            case "N":
                baseScore = Math.min(100, baseScore + 10);
                break;
            case "B1":
                baseScore = Math.max(60, baseScore - 5);
                break;
            case "B2":
                baseScore = Math.max(50, baseScore - 15);
                break;
            case "C":
                baseScore = Math.max(30, baseScore - 30);
                break;
            case "D":
                baseScore = Math.max(10, baseScore - 50);
                break;
        }

        return Math.min(100, Math.max(0, baseScore));
    }

    private String getHealthStatus(int score) {
        if (score >= 90)
            return "매우 좋음";
        if (score >= 70)
            return "좋음";
        if (score >= 50)
            return "보통";
        if (score >= 30)
            return "주의";
        return "위험";
    }

    private List<String> generateHealthTags(SampleData sample) {
        List<String> tags = new ArrayList<>();

        if (sample.normalPercent >= 70) {
            tags.add("심박수 정상");
        } else if (sample.normalPercent >= 40) {
            tags.add("주의 필요");
        } else {
            tags.add("검진 권장");
        }

        if (sample.avgRespiratoryRate >= 15 && sample.avgRespiratoryRate <= 30) {
            tags.add("호흡 안정");
        }

        if (!"D".equals(sample.mmvdStage) && !"C".equals(sample.mmvdStage)) {
            tags.add("활동량 우수");
        }

        return tags;
    }

    private String getHeartRateStatus(int rate, String petType) {
        if ("Cat".equalsIgnoreCase(petType)) {
            // 고양이: 150-240 BPM 정상
            if (rate >= 150 && rate <= 240)
                return "정상 범위";
            if (rate < 150)
                return "느림";
            return "빠름";
        } else {
            // 강아지: 60-140 BPM 정상 (크기에 따라 다름)
            if (rate >= 60 && rate <= 140)
                return "정상 범위";
            if (rate < 60)
                return "느림";
            return "빠름";
        }
    }

    private String getRespiratoryStatus(int rate) {
        // 강아지/고양이 정상 호흡수: 15-30회/분
        if (rate >= 15 && rate <= 30)
            return "안정적";
        return "불안정";
    }

    private String getConditionStatus(int score) {
        if (score >= 90)
            return "최상";
        if (score >= 70)
            return "양호";
        if (score >= 50)
            return "보통";
        return "주의";
    }

    private String getHeartRateZone(int rate) {
        if (rate >= 60 && rate <= 180)
            return "normal";
        if (rate >= 40 && rate <= 220)
            return "caution";
        return "danger";
    }

    private String getRespiratoryZone(int rate) {
        if (rate >= 15 && rate <= 30)
            return "normal";
        if (rate >= 10 && rate <= 40)
            return "caution";
        return "danger";
    }

    private String getRecommendation(String analysisResult) {
        switch (analysisResult) {
            case "정상":
                return "현재 상태를 유지하세요. 정기 검진을 권장합니다.";
            case "주의 요망":
                return "정확한 분석을 위해 다시 측정해주세요. 전문 수의사와 상담을 권장합니다.";
            case "이상 의심":
                return "즉시 동물병원 방문을 권장합니다.";
            default:
                return "측정 데이터를 확인해주세요.";
        }
    }

    /**
     * 33개 PDF 기반 샘플 데이터 초기화
     */
    private List<SampleData> initializeSampleDataset() {
        List<SampleData> dataset = new ArrayList<>();

        // 정상 케이스들 (10개)
        dataset.add(new SampleData("Dog", "2년 3개월", "골든 리트리버", "Male", 85, 22, 25.5, 85, "정상", "N"));
        dataset.add(new SampleData("Dog", "1년 8개월", "비글", "Female", 92, 20, 12.3, 92, "정상", "N"));
        dataset.add(new SampleData("Cat", "3년 2개월", "러시안 블루", "Male", 175, 24, 4.8, 88, "정상", "N"));
        dataset.add(new SampleData("Cat", "1년 6개월", "페르시안", "Female", 168, 22, 3.9, 90, "정상", "N"));
        dataset.add(new SampleData("Dog", "4년 1개월", "푸들", "Male", 88, 18, 6.2, 87, "정상", "N"));
        dataset.add(new SampleData("Dog", "2년 5개월", "시바견", "Female", 95, 21, 8.5, 91, "정상", "N"));
        dataset.add(new SampleData("Cat", "2년 9개월", "먼치킨", "Male", 182, 25, 3.2, 86, "정상", "N"));
        dataset.add(new SampleData("Dog", "5년 2개월", "말티즈", "Male", 102, 19, 3.8, 83, "정상", "N"));
        dataset.add(new SampleData("Cat", "4년 7개월", "브리티쉬 숏헤어", "Female", 170, 23, 5.1, 89, "정상", "N"));
        dataset.add(new SampleData("Dog", "3년 4개월", "웰시코기", "Male", 98, 20, 12.8, 85, "정상", "N"));

        // 주의 요망 케이스들 (15개)
        dataset.add(new SampleData("Cat", "1년 6개월", "미상", "Female", 193, 0, 0.0, 39, "주의 요망", "B2"));
        dataset.add(new SampleData("Dog", "6년 3개월", "치와와", "Male", 145, 28, 2.8, 45, "주의 요망", "B1"));
        dataset.add(new SampleData("Dog", "7년 8개월", "요크셔테리어", "Female", 138, 32, 3.1, 42, "주의 요망", "B2"));
        dataset.add(new SampleData("Cat", "5년 1개월", "아비시니안", "Male", 195, 35, 4.2, 38, "주의 요망", "B1"));
        dataset.add(new SampleData("Dog", "8년 2개월", "닥스훈트", "Male", 125, 30, 8.5, 48, "주의 요망", "B1"));
        dataset.add(new SampleData("Cat", "6년 5개월", "샴", "Female", 188, 28, 3.8, 52, "주의 요망", "B1"));
        dataset.add(new SampleData("Dog", "9년 1개월", "시추", "Male", 118, 26, 5.8, 55, "주의 요망", "B2"));
        dataset.add(new SampleData("Dog", "4년 6개월", "보더콜리", "Female", 105, 24, 18.2, 58, "주의 요망", "B1"));
        dataset.add(new SampleData("Cat", "7년 3개월", "터키시 앙고라", "Male", 178, 27, 4.5, 50, "주의 요망", "B1"));
        dataset.add(new SampleData("Dog", "10년 4개월", "포메라니안", "Female", 132, 29, 3.2, 44, "주의 요망", "B2"));
        dataset.add(new SampleData("Cat", "3년 8개월", "스코티시폴드", "Male", 185, 31, 4.0, 47, "주의 요망", "B1"));
        dataset.add(new SampleData("Dog", "5년 9개월", "진돗개", "Male", 108, 25, 20.5, 53, "주의 요망", "B1"));
        dataset.add(new SampleData("Cat", "8년 6개월", "노르웨이숲", "Female", 172, 33, 5.8, 41, "주의 요망", "B2"));
        dataset.add(new SampleData("Dog", "6년 7개월", "불독", "Male", 115, 35, 22.0, 40, "주의 요망", "B2"));
        dataset.add(new SampleData("Cat", "4년 2개월", "메인쿤", "Male", 165, 26, 7.2, 56, "주의 요망", "B1"));

        // 이상 의심 케이스들 (8개)
        dataset.add(new SampleData("Dog", "11년 5개월", "페키니즈", "Male", 155, 42, 4.5, 22, "이상 의심", "C"));
        dataset.add(new SampleData("Cat", "10년 2개월", "버만", "Female", 210, 45, 3.8, 18, "이상 의심", "C"));
        dataset.add(new SampleData("Dog", "12년 3개월", "파피용", "Female", 148, 40, 3.2, 25, "이상 의심", "C"));
        dataset.add(new SampleData("Cat", "9년 8개월", "렉돌", "Male", 205, 38, 6.5, 28, "이상 의심", "C"));
        dataset.add(new SampleData("Dog", "13년 1개월", "퍼그", "Male", 142, 48, 7.8, 15, "이상 의심", "D"));
        dataset.add(new SampleData("Cat", "11년 4개월", "페르시안", "Female", 198, 44, 4.2, 20, "이상 의심", "C"));
        dataset.add(new SampleData("Dog", "10년 9개월", "코카스파니엘", "Male", 135, 46, 12.5, 12, "이상 의심", "D"));
        dataset.add(new SampleData("Cat", "12년 6개월", "에그조틱", "Female", 215, 50, 3.5, 10, "이상 의심", "D"));

        return dataset;
    }

    /**
     * 샘플 데이터 내부 클래스
     */
    private static class SampleData {
        String species;
        String age;
        String breed;
        String sex;
        int avgHeartRate;
        int avgRespiratoryRate;
        double weight;
        int normalPercent;
        String analysisResult;
        String mmvdStage;

        SampleData(String species, String age, String breed, String sex,
                int avgHeartRate, int avgRespiratoryRate, double weight,
                int normalPercent, String analysisResult, String mmvdStage) {
            this.species = species;
            this.age = age;
            this.breed = breed;
            this.sex = sex;
            this.avgHeartRate = avgHeartRate;
            this.avgRespiratoryRate = avgRespiratoryRate;
            this.weight = weight;
            this.normalPercent = normalPercent;
            this.analysisResult = analysisResult;
            this.mmvdStage = mmvdStage;
        }
    }
}
