package com.petlog.healthcare.dto.meshy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Meshy.ai 3D 모델 생성 응답 DTO
 * WHY: Meshy.ai API 응답 데이터 구조화
 * 
 * @author healthcare-team
 * @since 2026-01-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meshy3DResponse {

    /**
     * Meshy 작업 ID (상태 조회용)
     */
    private String taskId;

    /**
     * 작업 상태 (queued, processing, succeeded, failed)
     */
    private String status;

    /**
     * 진행률 (0-100)
     */
    private Integer progress;

    /**
     * 안내 메시지
     */
    private String message;

    /**
     * 3D 모델 다운로드 URL (완료 시)
     */
    private String modelUrl;

    /**
     * 렌더링된 이미지 URL
     */
    private String renderedImageUrl;

    /**
     * 원본 이미지 URL (이미지→3D 생성 시)
     */
    private String sourceImageUrl;
}
