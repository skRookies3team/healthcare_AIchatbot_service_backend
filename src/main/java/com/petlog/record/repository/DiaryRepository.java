package com.petlog.record.repository;

import com.petlog.record.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 특정 사용자가 작성한 모든 일기 목록 조회
    List<Diary> findAllByUserId(Long userId);

    // 특정 펫에 대해 작성된 모든 일기 목록 조회
    List<Diary> findAllByPetId(Long petId);
}