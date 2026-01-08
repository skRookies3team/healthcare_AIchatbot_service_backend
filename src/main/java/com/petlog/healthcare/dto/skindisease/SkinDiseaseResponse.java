package com.petlog.healthcare.dto.skindisease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피부질환 분석 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinDiseaseResponse {

    private boolean success;
    private String message;
    private AnalysisResult result;
    private String imageUrl; // S3 저장된 이미지 URL
    private LocalDateTime analyzedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        private List<String> symptoms; // 발견된 증상
        private List<String> possibleDiseases; // 가능한 질환
        private String severity; // 심각도
        private String recommendation; // 권장 조치
        private String notes; // 추가 설명
        private String rawResponse; // AI 원본 응답
    }

    public static SkinDiseaseResponse success(AnalysisResult result) {
        return SkinDiseaseResponse.builder()
                .success(true)
                .message("분석이 완료되었습니다.")
                .result(result)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    public static SkinDiseaseResponse success(AnalysisResult result, String imageUrl) {
        return SkinDiseaseResponse.builder()
                .success(true)
                .message("분석이 완료되었습니다.")
                .result(result)
                .imageUrl(imageUrl)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    public static SkinDiseaseResponse error(String message) {
        return SkinDiseaseResponse.builder()
                .success(false)
                .message(message)
                .analyzedAt(LocalDateTime.now())
                .build();
    }
}
