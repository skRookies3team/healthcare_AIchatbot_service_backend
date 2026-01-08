package com.petlog.healthcare.dto.vet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 수의사 지식 검색 결과 DTO
 * WHY: RAG 검색 결과를 담아 챗봇에 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetQAResult {

    /**
     * 지식 ID
     */
    private Long id;

    /**
     * 진료과
     */
    private String department;

    /**
     * 질병 분류
     */
    private String disease;

    /**
     * 생애 주기
     */
    private String lifeCycle;

    /**
     * 원본 질문
     */
    private String question;

    /**
     * 수의사 답변
     */
    private String answer;

    /**
     * 유사도 점수 (0.0 ~ 1.0)
     */
    private float similarityScore;

    /**
     * RAG Context 생성용 포맷
     */
    public String toContext() {
        return String.format(
                "[%s - %s]\nQ: %s\nA: %s",
                department,
                disease != null ? disease : "일반",
                truncate(question, 200),
                truncate(answer, 500));
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
