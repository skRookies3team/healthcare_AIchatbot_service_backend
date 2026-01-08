package com.petlog.healthcare.controller;

import com.petlog.healthcare.dto.SttResponse;
import com.petlog.healthcare.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/chat/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    @PostMapping
    public ResponseEntity<SttResponse> transcribeAudio(@RequestParam("file") MultipartFile file) {
        log.info("📥 STT 컨트롤러 요청 받음: {}", file.getOriginalFilename());
        SttResponse response = sttService.transcribe(file);
        return ResponseEntity.ok(response);
    }
}
