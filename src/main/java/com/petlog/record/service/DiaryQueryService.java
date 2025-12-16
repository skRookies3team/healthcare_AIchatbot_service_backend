package com.petlog.record.service;

import com.petlog.record.dto.response.DiaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface DiaryQueryService {

    // 캘린더용: 특정 날짜의 일기 조회
    List<DiaryResponse> getDiariesByDate(Long userId, LocalDate date);

    // 보관함용: AI 다이어리 전체 조회
    List<DiaryResponse> getAiDiaries(Long userId);
}