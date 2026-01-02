package com.petlog.healthcare.controller;

import com.petlog.healthcare.service.PetPersonaService;
import com.petlog.healthcare.service.ClaudeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Pet Persona Chatbot API
 *
 * [특징]
 * - 반려동물이 직접 대화하는 페르소나 챗봇
 * - Diary 벡터 기반 "기억" 활용
 * - 1인칭 화법 ("나", "내가")
 *
 * [vs 일반 챗봇]
 * - /api/chat/health: 수의사 역할 (라이펫 문서)
 * - /api/chat/persona: 반려동물 역할 (Diary 벡터) ← 이것!
 *
 * @author healthcare-team
 * @since 2025-01-02
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Pet Persona Chatbot", description = "반려동물 페르소나 대화 API")
public class PetPersonaController {

    private final PetPersonaService petPersonaService;

    /**
     * Pet Persona 대화 API
     *
     * [요청 예시]
     * POST /api/chat/persona
     * {
     *   "petId": 1,
     *   "message": "몽치야, 오늘 기분 어때?"
     * }
     *
     * [응답 예시]
     * {
     *   "response": "좋아! 🐾 지난주에 산책 갔던 공원 또 가고 싶어!",
     *   "petName": "몽치"
     * }
     */
    @Operation(
            summary = "Pet Persona 대화",
            description = "반려동물이 직접 대화하는 페르소나 챗봇 (Diary 벡터 기반)"
    )
    @PostMapping("/persona")
    public ResponseEntity<Map<String, String>> chatWithPet(@RequestBody PersonaRequest request) {
        log.info("🐾 Persona 대화 요청 - petId: {}, message: '{}'",
                request.petId(), request.message());

        // 입력 검증
        if (request.petId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "petId는 필수입니다"));
        }

        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "message는 필수입니다"));
        }

        try {
            // Persona 챗봇 호출
            String response = petPersonaService.chat(request.petId(), request.message());

            return ResponseEntity.ok(Map.of(
                    "response", response,
                    "petId", request.petId().toString()
            ));

        } catch (Exception e) {
            log.error("❌ Persona 대화 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "대화 처리 중 오류가 발생했습니다"));
        }
    }

    /**
     * 요청 DTO (Java Record)
     */
    public record PersonaRequest(
            Long petId,    // 반려동물 ID (필수)
            String message // 사용자 메시지 (필수)
    ) {}
}