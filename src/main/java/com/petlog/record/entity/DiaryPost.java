package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "diary_posts")
public class DiaryPost {

    // 게시글 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID (FK)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 펫 ID (FK)
    @Column(name = "pet_id")
    private Long petId;

    // 스타일 ID (DiaryStyle 참조)
    // @ManyToOne(fetch = FetchType.LAZY) // JPA 매핑 대신 ID만 저장하는 구조로 구현
    @Column(name = "style_id")
    private Long styleId;

    // 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    // 이미지 URL 배열 (JSON 타입으로 저장될 것으로 가정)
    // PostgreSql의 JSONB 타입 또는 JSON 문자열로 처리
    @Column(name = "images", columnDefinition = "JSON")
    private String imagesJson; // 이미지 URL 배열을 JSON 문자열로 저장

    // 위치 정보
    @Column(name = "location", length = 255)
    private String location;

    // 작성일
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 수정일
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}