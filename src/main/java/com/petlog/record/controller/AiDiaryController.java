//package com.petlog.record.controller;
//
//import com.petlog.record.dto.request.DiaryRequest;
//import com.petlog.record.dto.response.DiaryResponse;
//import com.petlog.record.entity.Diary;
//import com.petlog.record.service.AiDiaryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestPart;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//@Slf4j
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/diaries/ai")
//public class AiDiaryController {
//
//    private final AiDiaryService aiDiaryService;
//
//    /**
//     * AI 일기 생성 API
//     * [POST] /api/diaries/ai
//     * Content-Type: multipart/form-data
//     * @param image : 이미지 파일
//     * @param request : JSON 데이터 (DiaryRequest.Create 타입으로 매핑)
//     */
//    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    public ResponseEntity<DiaryResponse> createAiDiary(
//            @RequestPart(value = "image") MultipartFile image,
//            // [수정] DiaryRequest -> DiaryRequest.Create 로 변경
//            @RequestPart(value = "data") DiaryRequest.Create request
//    ) {
//        // 임시 유저 ID
//        Long userId = 1L;
//
//        log.info("AI 일기 생성 요청 - PetId: {}, UserId: {}", request.getPetId(), userId);
//
//        // 서비스 호출 시 필요한 값 전달
//        Diary savedDiary = aiDiaryService.createAiDiary(
//                userId,
//                request.getPetId(),
//                image,
//                request.getVisibility()
//        );
//
//        return ResponseEntity.ok(DiaryResponse.fromEntity(savedDiary));
//    }
//}