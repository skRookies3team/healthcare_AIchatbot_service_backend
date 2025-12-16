package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder // 클래스 레벨로 이동: 모든 필드를 빌더에 포함
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 추가: 빌더 패턴 사용 시 모든 필드를 받는 생성자가 필요
@Table(name = "DIARY_IMAGES")
public class DiaryImage {

    // 이미지 ID
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    // 일기 (FK) - 일기가 없는 이미지는 존재할 수 없음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    // [추가] 사진이 어느 사용자에게 귀속되는지 (보관함 역할)
    @Column(nullable = false)
    private Long userId;

    // 이미지 경로 - 필수값
    @Column(nullable = false)
    private String imageUrl;

    // 순서 - 필수값
    @Column(nullable = false)
    private Integer imgOrder;

    // 대표 이미지 여부 - 필수값
    @Column(nullable = false)
    private Boolean mainImage;

    // [추가] 이미지 출처 (GALLERY, ARCHIVE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageSource source;

    // 생성일
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 연관관계 편의 메서드
    public void setDiary(Diary diary) {
        this.diary = diary;
    }
}