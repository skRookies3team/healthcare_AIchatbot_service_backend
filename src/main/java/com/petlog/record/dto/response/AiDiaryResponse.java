package com.petlog.record.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class AiDiaryResponse {
    
    // AI가 생성한 일기 본문
    @JsonProperty("content")
    private String content;

    // AI가 분석한 기분 (예: "행복", "나른함")
    @JsonProperty("mood")
    private String mood;
}