package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryStyleRequest;
import com.petlog.record.dto.response.DiaryStyleResponse;
import com.petlog.record.service.DiaryStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Diary Style API", description = "다이어리 스타일 설정(폰트, 테마 등) 관리 API")
@RestController
@RequestMapping("/api/v1/diary/styles")
@RequiredArgsConstructor
public class DiaryStyleController {

    private final DiaryStyleService diaryStyleService;

    // 스타일 설정 저장 (POST)
    // Upsert 로직을 포함하여, 없으면 생성하고 있으면 업데이트함
    @Operation(summary = "스타일 설정 생성/수정", description = "사용자 또는 특정 펫의 다이어리 스타일 설정을 저장합니다. (기존 설정이 있으면 업데이트)")
    @PostMapping
    public ResponseEntity<DiaryStyleResponse> createStyle(
            @RequestBody DiaryStyleRequest request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.createOrUpdateStyle(userId, request);
        return ResponseEntity.ok(response);
    }

    // 스타일 설정 수정 (PUT)
    @Operation(summary = "스타일 설정 수정", description = "특정 스타일 ID에 대한 설정을 수정합니다.")
    @PutMapping("/{styleId}")
    public ResponseEntity<DiaryStyleResponse> updateStyle(
            @PathVariable Long styleId,
            @RequestBody DiaryStyleRequest request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.updateStyle(styleId, request, userId);
        return ResponseEntity.ok(response);
    }

    // 사용자의 현재 스타일 설정 조회
    @Operation(summary = "내 스타일 조회", description = "사용자의 현재 스타일 설정을 조회합니다. (펫 ID 파라미터 선택 가능)")
    @GetMapping("/me")
    public ResponseEntity<DiaryStyleResponse> getMyStyle(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(required = false) Long petId
    ) {
        DiaryStyleResponse response = diaryStyleService.getUserStyle(userId, petId);
        return ResponseEntity.ok(response);
    }

    // 특정 펫의 스타일 설정 조회
    @Operation(summary = "펫 스타일 조회", description = "특정 펫의 스타일 설정을 조회합니다.")
    @GetMapping("/pet/{petId}")
    public ResponseEntity<DiaryStyleResponse> getPetStyle(
            @PathVariable Long petId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        DiaryStyleResponse response = diaryStyleService.getPetStyle(petId, userId);
        return ResponseEntity.ok(response);
    }
}