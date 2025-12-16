package com.petlog.record.repository;

import com.petlog.record.entity.DiaryStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DiaryStyleRepository extends JpaRepository<DiaryStyle, Long> {

    // [핵심] 스타일 생성/조회 시 사용: 특정 사용자의 특정 펫에 대한 스타일을 찾음
    Optional<DiaryStyle> findByUserIdAndPetId(Long userId, Long petId);
    
    // 사용자가 소유한 모든 스타일 설정을 조회할 때 사용 가능
    List<DiaryStyle> findAllByUserId(Long userId);
    
    // 펫 ID 없이 사용자 전체의 기본 스타일을 찾을 때 사용 가능
    Optional<DiaryStyle> findByUserIdAndPetIdIsNull(Long userId);
}