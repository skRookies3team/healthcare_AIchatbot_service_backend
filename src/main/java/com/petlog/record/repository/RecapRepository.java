package com.petlog.record.repository;

import com.petlog.record.entity.Recap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecapRepository extends JpaRepository<Recap, Long> {
    
    // 1. 사용자별 리캡 전체 조회 (최신순)
    List<Recap> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 2. 펫별 리캡 조회 (최신순)
    List<Recap> findAllByPetIdOrderByCreatedAtDesc(Long petId);
}