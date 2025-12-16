package com.petlog.record.repository;

import com.petlog.record.entity.DiaryPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiaryPostRepository extends JpaRepository<DiaryPost, Long> {
    
    // 필요하다면 여기에 커스텀 쿼리 메서드를 추가할 수 있습니다.
    // 예: List<DiaryPost> findAllByUserIdAndPetId(Long userId, Long petId);
    
    // 이미지를 JSON 문자열로 저장하는 경우, 필요한 조회 쿼리 추가 가능
}