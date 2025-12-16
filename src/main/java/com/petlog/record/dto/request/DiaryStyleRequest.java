package com.petlog.record.dto.request;

import com.petlog.record.entity.DiaryStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다이어리 스타일 설정 요청 DTO")
public class DiaryStyleRequest {

    @Schema(description = "갤러리 레이아웃 타입 (grid, masonry, slider, classic)", example = "masonry")
    private String galleryType;

    @Schema(description = "텍스트 정렬 방식 (left, center, right)", example = "left")
    private String textAlignment;

    @Schema(description = "폰트 크기 (px 단위)", example = "16")
    private Integer fontSize;

    @Schema(description = "이미지 크기 옵션 (small, medium, large)", example = "medium")
    private String sizeOption;

    @Schema(description = "배경색 (HEX 코드)", example = "#FFFFFF")
    private String backgroundColor;

    @Schema(description = "적용할 프리셋 이름", example = "cozy_morning")
    private String preset;

    @Schema(description = "테마 스타일 (basic, romantic, modern 등)", example = "modern")
    private String themeStyle;

    @Schema(description = "스타일을 적용할 펫 ID (선택값: null일 경우 유저의 기본 스타일로 저장)", example = "1")
    private Long petId;

    // DTO -> Entity 변환
    public DiaryStyle toEntity(Long userId) {
        return DiaryStyle.builder()
                .userId(userId)
                .petId(this.petId)
                .galleryType(this.galleryType)
                .textAlignment(this.textAlignment)
                .fontSize(this.fontSize)
                .sizeOption(this.sizeOption)
                .backgroundColor(this.backgroundColor)
                .preset(this.preset)
                .themeStyle(this.themeStyle)
                .build();
    }
}