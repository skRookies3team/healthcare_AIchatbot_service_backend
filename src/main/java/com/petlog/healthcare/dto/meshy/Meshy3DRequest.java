package com.petlog.healthcare.dto.meshy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Meshy.ai 3D 모델 생성 요청 DTO
 * WHY: Meshy.ai API 요청 데이터 구조화
 * 
 * @author healthcare-team
 * @since 2026-01-08
 */
public class Meshy3DRequest {

    /**
     * 텍스트 → 3D 모델 생성 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextToModel {
        private String prompt;
    }

    /**
     * 이미지 → 3D 모델 생성 요청
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageToModel {
        private String imageUrl;
    }
}
