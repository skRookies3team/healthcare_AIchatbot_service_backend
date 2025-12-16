package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "RECAPS")
public class Recap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recapId;

    @Column(nullable = false)
    private Long petId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title; // 예: "2024년 1-2월"

    @Column(columnDefinition = "TEXT")
    private String summary; // 메인 요약 멘트

    private LocalDate periodStart; // 기간 시작
    private LocalDate periodEnd;   // 기간 끝

    private String mainImageUrl;   // 배경 이미지

    // === [리스트 카드 UI용 데이터] ===
    private Integer momentCount; // 예: 45 (45개의 순간)
    
    @Enumerated(EnumType.STRING)
    private RecapStatus status; // GENERATED(완료), WAITING(대기중)

    // (헬스케어 서비스에서 별도로 조회하여 DTO에 병합함)

    // === [하이라이트 목록 (1:N)] ===
    @Builder.Default
    @OneToMany(mappedBy = "recap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecapHighlight> highlights = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 연관관계 메서드
    public void addHighlight(RecapHighlight highlight) {
        this.highlights.add(highlight);
        highlight.setRecap(this);
    }
}

