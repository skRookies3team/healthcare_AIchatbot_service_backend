package com.petlog.record.service;

import com.petlog.record.dto.request.DiaryStyleRequest; // DTO 경로는 프로젝트에 맞게 수정 필요
import com.petlog.record.dto.response.DiaryStyleResponse; // DTO 경로는 프로젝트에 맞게 수정 필요

// 이전에 사용하셨던 com.petlog.record 패키지 경로를 가정합니다.
public interface DiaryStyleService {

    // 스타일 생성/수정 (Upsert 로직 포함)
    DiaryStyleResponse createOrUpdateStyle(Long userId, DiaryStyleRequest request);

    // 스타일 설정 수정 (ID 기반)
    DiaryStyleResponse updateStyle(Long styleId, DiaryStyleRequest request, Long userId);

    // 사용자의 현재 스타일 설정 조회 (기본 스타일 생성 로직 포함)
    DiaryStyleResponse getUserStyle(Long userId, Long petId);

    // 특정 펫의 스타일 설정 조회 (getUserStyle과 동일 로직으로 처리 가정)
    DiaryStyleResponse getPetStyle(Long petId, Long userId);
}