package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "RECAP_HIGHLIGHTS")
public class RecapHighlight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long highlightId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recap_id")
    private Recap recap;

    private String title;   // 예: "첫 눈 산책"
    private String content; // 예: "생애 처음 보는 눈에 신나서..."

    // 생성일
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void setRecap(Recap recap) {
        this.recap = recap;
    }
}