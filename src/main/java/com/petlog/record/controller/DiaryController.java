package com.petlog.record.controller;

import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 추가
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j // 로그 사용을 위해 추가
@Tag(name = "Diary API", description = "다이어리 CRUD 및 관리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService1;

    /**
     * [AI 일기 생성]
     * - 이미지 파일과 JSON 데이터(userId, petId 등)를 함께 받습니다.
     * - Content-Type: multipart/form-data
     */
    @Operation(summary = "AI 일기 생성", description = "이미지를 분석하여 AI가 일기를 작성합니다.")
    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createAiDiary(
            @Parameter(description = "업로드할 이미지 파일", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("image") MultipartFile image,

            @Parameter(description = "일기 생성 요청 데이터 (JSON)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @Valid @RequestPart("data") DiaryRequest.Create request
    ) {
        log.info("AI 일기 생성 요청 - UserId: {}, PetId: {}", request.getUserId(), request.getPetId());

        // 1. 서비스 호출 (통합된 DiaryService 사용)
        Long diaryId = diaryService1.createAiDiary(
                request.getUserId(),
                request.getPetId(),
                image,
                request.getVisibility()
        );

        // 2. 응답 메시지 커스텀 (Map 사용 - 기존 스타일 유지)
        Map<String, Object> response = new HashMap<>();
        response.put("diaryId", diaryId);
        response.put("message", "AI 일기가 성공적으로 생성되었습니다.");

        // 3. 201 Created 반환
        return ResponseEntity
                .created(URI.create("/api/diaries/" + diaryId))
                .body(response);
    }

    // ... 기존 조회(getDiary), 수정(updateDiary), 삭제(deleteDiary) 메서드는 그대로 유지 ...


    @Operation(summary = "다이어리 상세 조회", description = "다이어리 ID를 통해 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        // [디버깅 로그] 요청이 들어오는지 확인
        log.info("GET Diary Request - ID: {}", diaryId);
        return ResponseEntity.ok(diaryService1.getDiary(diaryId));
    }

    @Operation(summary = "다이어리 수정", description = "기존 일기의 내용(텍스트, 공개범위, 날씨, 기분)을 부분 수정합니다.")
    @PatchMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(@PathVariable Long diaryId,
                                            @RequestBody DiaryRequest.Update request) {
        diaryService1.updateDiary(diaryId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "다이어리 삭제", description = "특정 일기를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long diaryId) {
        diaryService1.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}