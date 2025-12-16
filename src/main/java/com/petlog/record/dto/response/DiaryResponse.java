package com.petlog.record.dto.response;

import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다이어리 상세 조회 응답 DTO")
public class DiaryResponse {

    @Schema(description = "다이어리 ID", example = "1")
    private Long diaryId;

    @Schema(description = "작성자(사용자) ID", example = "1")
    private Long userId;

    @Schema(description = "관련 펫 ID", example = "1")
    private Long petId;

    @Schema(description = "일기 내용", example = "오늘 공원에서 산책하며 즐거운 시간을 보냈다.")
    private String content;

    @Schema(description = "공개 범위", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "날씨", example = "맑음")
    private String weather;

    @Schema(description = "기분", example = "행복")
    private String mood;

    @Schema(description = "AI 생성 여부", example = "false")
    private Boolean isAiGen;

    @Schema(description = "작성일시", example = "2023-10-10T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2023-10-10T12:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "첨부 이미지 목록")
    private List<Image> images;

    // [핵심] Entity -> DTO 변환 로직 (Service에서 호출)
    public static DiaryResponse fromEntity(Diary diary) {
        return DiaryResponse.builder()
                .diaryId(diary.getDiaryId())
                .userId(diary.getUserId())
                .petId(diary.getPetId())
                .content(diary.getContent())
                .visibility(diary.getVisibility())
                .weather(diary.getWeather())
                .mood(diary.getMood())
                .isAiGen(diary.getIsAiGen())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .images(diary.getImages().stream()
                        .map(Image::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    // [Inner DTO] 이미지 응답용
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "다이어리 이미지 정보 DTO")
    public static class Image {

        @Schema(description = "이미지 ID", example = "10")
        private Long imageId;

        @Schema(description = "이미지 URL", example = "https://bucket.s3.region.amazonaws.com/image.jpg")
        private String imageUrl;

        @Schema(description = "이미지 순서", example = "1")
        private Integer imgOrder;

        @Schema(description = "대표 이미지 여부", example = "true")
        private Boolean mainImage;

        // Entity -> DTO 변환
        public static Image fromEntity(DiaryImage image) {
            return Image.builder()
                    .imageId(image.getImageId())
                    .imageUrl(image.getImageUrl())
                    .imgOrder(image.getImgOrder())
                    .mainImage(image.getMainImage())
                    .build();
        }
    }
}