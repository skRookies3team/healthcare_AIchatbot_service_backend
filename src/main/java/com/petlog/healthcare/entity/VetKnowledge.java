package com.petlog.healthcare.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 수의사 지식 베이스 Entity
 * WHY: RAG 검색을 위한 Q&A 데이터 저장
 *
 * 데이터 출처: AI Hub 반려견 성장 및 질병 관련 말뭉치 데이터
 */
@Entity
@Table(name = "vet_knowledge", indexes = {
        @Index(name = "idx_department", columnList = "department"),
        @Index(name = "idx_disease", columnList = "disease"),
        @Index(name = "idx_life_cycle", columnList = "lifeCycle")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 진료과 (내과/피부과/안과/치과)
     */
    @Column(nullable = false, length = 50)
    private String department;

    /**
     * 질병 분류
     */
    @Column(length = 100)
    private String disease;

    /**
     * 생애 주기 (유아견/성견/노령견)
     */
    @Column(length = 50)
    private String lifeCycle;

    /**
     * 보호자 질문
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    /**
     * 수의사 답변
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    /**
     * 원본 instruction (수의사 역할 지시)
     */
    @Column(columnDefinition = "TEXT")
    private String instruction;

    /**
     * 원본 파일명
     */
    @Column(length = 255)
    private String sourceFile;

    /**
     * Milvus 벡터 ID (임베딩 저장용)
     */
    @Column(length = 100)
    private String milvusId;

    /**
     * 생성 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
