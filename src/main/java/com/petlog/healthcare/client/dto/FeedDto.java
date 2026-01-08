package com.petlog.healthcare.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed DTO (Social Service 응답)
 *
 * 3D 모델 생성 시 이미지 URL 추출용
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedDto {

    private Long feedId;
    private Long writerId;
    private String petName;
    private String content;

    /**
     * 피드 이미지 URL 목록 (S3)
     * 3D 모델 생성에 사용
     */
    private List<String> imageUrls;

    private String location;
    private long likeCount;
    private Long commentCount;
    private List<String> hashtags;
    private LocalDateTime createdAt;
}
