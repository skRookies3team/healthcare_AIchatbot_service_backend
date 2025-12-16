package com.petlog.record.repository;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiaryQueryRepository extends JpaRepository<Diary, Long> {

    // 1. 캘린더용 날짜별 조회 (해당 날짜의 00:00:00 ~ 23:59:59 사이 데이터 검색)
    List<Diary> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // 2. AI 다이어리 보관함 조회 (isAiGen = true 인 것만)
    List<Diary> findAllByUserIdAndIsAiGenOrderByCreatedAtDesc(Long userId, Boolean isAiGen);
}