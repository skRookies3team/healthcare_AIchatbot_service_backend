package com.petlog.healthcare.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.petlog.healthcare.service.ClaudeService;

import java.util.Map;

/**
 * Healthcare AI Chatbot REST API
 * POST /api/chat - Claude 3.5 Haiku 상담
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ClaudeService claudeService;

    /**
     * AI 챗봇 상담 API
     *
     * @param request {"message": "강아지가 밥을 안 먹어요"}
     * @return {"response": "Claude 답변"}
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = claudeService.chat(message);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
