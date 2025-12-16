package com.petlog.record.controller;

import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Diary Query API", description = "다이어리 조회(캘린더/보관함) API") // 컨트롤러 설명 추가
@RestController
@RequestMapping("/api/diary-queries")
@RequiredArgsConstructor
public class DiaryQueryController {

    private final DiaryQueryService diaryQueryService;

    // === 캘린더 날짜별 조회 API ===
    @Operation(summary = "캘린더 날짜별 조회", description = "특정 사용자의 특정 날짜에 작성된 다이어리 목록을 조회합니다.")
    @GetMapping("/calendar")
    public ResponseEntity<List<DiaryResponse>> getDiariesByDate(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(diaryQueryService.getDiariesByDate(userId, date));
    }

    // === AI 다이어리 보관함 조회 API ===
    @Operation(summary = "AI 다이어리 보관함 조회", description = "특정 사용자가 AI를 통해 생성한 다이어리 목록(isAiGen=true)을 조회합니다.")
    @GetMapping("/ai-archive")
    public ResponseEntity<List<DiaryResponse>> getAiDiaries(@RequestParam Long userId) {
        return ResponseEntity.ok(diaryQueryService.getAiDiaries(userId));
    }
}