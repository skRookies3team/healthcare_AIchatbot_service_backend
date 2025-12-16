package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder // 클래스 레벨로 이동: 모든 필드를 대상으로 빌더 생성 가능
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // 빌더 패턴 사용 시 전체 생성자가 필요
@Table(name = "DIARIES")
public class Diary {

    // 일기 ID
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    // 작성자 ID (FK) - 필수값
    @Column(nullable = false)
    private Long userId;

    // 펫 ID (FK) - 필수값
    @Column(nullable = false)
    private Long petId;

    // 일기 내용 (TEXT 타입, 사진만 올릴 수 있으므로 null 허용)
    @Column(columnDefinition = "TEXT")
    private String content;

    // 공개 범위 (PUBLIC, FOLLOWER, PRIVATE) - 필수값
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    // AI 작성 여부 - 필수값
    @Column(nullable = false)
    private Boolean isAiGen;

    // 날씨 (선택 입력)
    private String weather;

    // 기분 (선택 입력)
    private String mood;

    // 보관함 ID (선택 입력)
    private Long photoArchiveId;

    // 작성일
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 일기 이미지 목록
    // @Builder.Default: 빌더로 생성할 때도 이 필드가 null이 아닌 빈 리스트(new ArrayList)로 초기화됨
    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryImage> images = new ArrayList<>();

    // === [비즈니스 로직] ===
    public void update(String content, Visibility visibility, String weather, String mood) {
        this.content = content;
        this.visibility = visibility;
        this.weather = weather;
        this.mood = mood;
    }

    public void addImage(DiaryImage image) {
        this.images.add(image);
        image.setDiary(this);
    }
}