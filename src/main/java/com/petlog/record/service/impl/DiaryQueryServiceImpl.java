package com.petlog.record.service.impl;

import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.repository.DiaryQueryRepository;
import com.petlog.record.service.DiaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryServiceImpl implements DiaryQueryService {

    private final DiaryQueryRepository diaryQueryRepository;

    @Override
    public List<DiaryResponse> getDiariesByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Diary> diaries = diaryQueryRepository.findAllByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);

        return diaries.stream()
                .map(DiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiaryResponse> getAiDiaries(Long userId) {
        // isAiGen = true 인 데이터만 조회하도록 호출
        List<Diary> aiDiaries = diaryQueryRepository.findAllByUserIdAndIsAiGenOrderByCreatedAtDesc(userId, true);

        return aiDiaries.stream()
                .map(DiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}