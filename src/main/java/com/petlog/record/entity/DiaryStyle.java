package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter // Service에서 Dirty Checking을 위해 필요
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "diary_styles")
public class DiaryStyle {

    // 스타일 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID (FK)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 펫 ID (FK)
    @Column(name = "pet_id")
    private Long petId;

    // 갤러리 스타일
    @Column(name = "gallery_type", nullable = false, length = 50)
    @Builder.Default
    private String galleryType = "grid";

    // 텍스트 정렬
    @Column(name = "text_alignment", nullable = false, length = 20)
    @Builder.Default
    private String textAlignment = "left";

    // 글자 크기
    @Column(name = "font_size", nullable = false)
    @Builder.Default
    private Integer fontSize = 16;

    // 작게/크게 옵션
    @Column(name = "size_option", nullable = false, length = 20)
    @Builder.Default
    private String sizeOption = "medium";

    // 배경 색상
    @Column(name = "background_color", nullable = false, length = 7)
    @Builder.Default
    private String backgroundColor = "#FFFFFF";

    // 프리셋
    @Column(name = "preset", nullable = false, length = 50)
    @Builder.Default
    private String preset = "default";

    // 기본/로맨틱/모던 스타일
    @Column(name = "theme_style", nullable = false, length = 50)
    @Builder.Default
    private String themeStyle = "basic";

    // 작성일
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 참고: Foreign Key는 논리적으로만 존재하며, JPA는 @ManyToOne 등으로 매핑함. 
    // 여기서는 ID만 저장하는 구조를 따릅니다.
}