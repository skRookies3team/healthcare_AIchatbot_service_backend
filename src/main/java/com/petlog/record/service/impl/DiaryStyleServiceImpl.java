package com.petlog.record.service.impl;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.entity.DiaryStyle;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.exception.ResourceNotFoundException;
import com.petlog.record.exception.UnauthorizedException;
import com.petlog.record.repository.DiaryStyleRepository;
import com.petlog.record.service.DiaryStyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨: 기본은 읽기 전용
public class DiaryStyleServiceImpl implements DiaryStyleService {

    private final DiaryStyleRepository diaryStyleRepository;

    @Transactional // [수정] 쓰기 트랜잭션 부여
    public DiaryStyle createDefaultStyle(Long userId, Long petId) {
        DiaryStyle defaultStyle = DiaryStyle.builder()
                .userId(userId)
                .petId(petId)
                .galleryType("grid")
                .textAlignment("left")
                .fontSize(16)
                .sizeOption("medium")
                .backgroundColor("#FFFFFF")
                .preset("default")
                .themeStyle("basic")
                .build();

        // save 호출
        return diaryStyleRepository.save(defaultStyle);
    }


    @Override
    @Transactional // 쓰기 트랜잭션 (데이터 생성/수정 가능)
    public DiaryStyleResponse createOrUpdateStyle(Long userId, DiaryStyleRequest request) {
        // [Upsert 로직] 기존 스타일이 (UserId, PetId) 조합으로 있는지 확인
        Optional<DiaryStyle> existingStyle = diaryStyleRepository
                .findByUserIdAndPetId(userId, request.getPetId());

        if (existingStyle.isPresent()) {
            // 있으면 업데이트
            return updateStyle(existingStyle.get().getId(), request, userId);
        }

        // 없으면 새로 생성 (CREATE) - DTO의 toEntity 메서드 사용
        DiaryStyle style = request.toEntity(userId);

        DiaryStyle saved = diaryStyleRepository.save(style);
        return DiaryStyleResponse.fromEntity(saved);
    }

    @Override
    @Transactional // 쓰기 트랜잭션 (데이터 수정 가능)
    public DiaryStyleResponse updateStyle(Long styleId, DiaryStyleRequest request, Long userId) {
        DiaryStyle style = diaryStyleRepository.findById(styleId)
                // [수정] ResourceNotFoundException에 ErrorCode를 함께 전달
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIARY_NOT_FOUND, "Style not found"));

        // 권한 확인
        if (!style.getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }

        // 업데이트 (PUT 방식 로직 유지)
        if (request.getGalleryType() != null) {
            style.setGalleryType(request.getGalleryType());
        }
        if (request.getTextAlignment() != null) {
            style.setTextAlignment(request.getTextAlignment());
        }
        if (request.getFontSize() != null) {
            style.setFontSize(request.getFontSize());
        }
        if (request.getSizeOption() != null) {
            style.setSizeOption(request.getSizeOption());
        }
        if (request.getBackgroundColor() != null) {
            style.setBackgroundColor(request.getBackgroundColor());
        }
        if (request.getPreset() != null) {
            style.setPreset(request.getPreset());
        }
        if (request.getThemeStyle() != null) {
            style.setThemeStyle(request.getThemeStyle());
        }

        // Dirty Checking으로 트랜잭션 종료 시 반영됨
        return DiaryStyleResponse.fromEntity(style);
    }

    // [수정] READ-ONLY 트랜잭션 유지 (INSERT는 createDefaultStyle의 새 트랜잭션에 위임)
    // orElseGet에서 createDefaultStyle(public + @Transactional)을 호출
    @Override
    @Transactional(readOnly = true)
    public DiaryStyleResponse getUserStyle(Long userId, Long petId) {
        DiaryStyle style = diaryStyleRepository
                .findByUserIdAndPetId(userId, petId)
                .orElseGet(() -> createDefaultStyle(userId, petId)); // 새로운 트랜잭션으로 분리되어 호출

        return DiaryStyleResponse.fromEntity(style);
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryStyleResponse getPetStyle(Long petId, Long userId) {
        return getUserStyle(userId, petId);
    }
}