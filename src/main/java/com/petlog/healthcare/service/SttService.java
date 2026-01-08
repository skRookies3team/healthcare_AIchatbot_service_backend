package com.petlog.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlog.healthcare.dto.SttResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * STT (Speech-to-Text) 서비스
 * WHY: OpenAI Whisper API를 사용하여 음성을 텍스트로 변환
 * OkHttp를 사용하여 multipart/form-data 요청을 안정적으로 처리
 */
@Slf4j
@Service
public class SttService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SttService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * API 키 로드 검증 (서버 시작 시 확인)
     * WHY: 설정 오류를 조기에 발견하기 위함
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            log.error("❌ OpenAI API Key가 설정되지 않았습니다! openai.api.key 설정을 확인하세요.");
        } else {
            String maskedKey = openaiApiKey.length() > 8
                    ? openaiApiKey.substring(0, 8) + "..."
                    : "***";
            log.info("✅ OpenAI API Key 로드 완료: {}", maskedKey);
        }
    }

    /**
     * 음성 파일을 텍스트로 변환
     * 
     * @param file 음성 파일 (mp3, wav, m4a, webm 등)
     * @return 변환된 텍스트
     */
    public SttResponse transcribe(MultipartFile file) {
        log.info("🎤 STT 요청: file={}, size={}", file.getOriginalFilename(), file.getSize());

        File tempFile = null;
        try {
            // 1. 임시 파일 생성
            String originalFilename = file.getOriginalFilename();
            String extension = ".mp3"; // 기본값
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            tempFile = File.createTempFile("stt_", extension);
            file.transferTo(tempFile);

            // 2. MediaType 결정
            MediaType mediaType = getMediaType(extension);

            // 3. OkHttp 요청 생성
            RequestBody fileBody = RequestBody.create(tempFile, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", originalFilename, fileBody)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("language", "ko")
                    .build();

            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .header("Authorization", "Bearer " + openaiApiKey.trim())
                    .post(requestBody)
                    .build();

            // 4. API 호출
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("❌ OpenAI API 오류: {} - {}", response.code(), responseBody);
                    throw new RuntimeException("OpenAI API 오류: " + response.code());
                }

                // 5. JSON 파싱
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String text = jsonNode.get("text").asText();

                log.info("✅ STT 변환 성공: {}", text);
                return new SttResponse(text);
            }

        } catch (IOException e) {
            log.error("❌ 파일 처리 중 오류 발생", e);
            throw new RuntimeException("오디오 파일 처리 실패", e);
        } catch (Exception e) {
            log.error("❌ STT 변환 중 오류 발생", e);
            throw new RuntimeException("STT 변환 실패: " + e.getMessage(), e);
        } finally {
            // 6. 임시 파일 삭제
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("⚠️ 임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 확장자에 따른 MediaType 반환
     */
    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".mp3" -> MediaType.parse("audio/mpeg");
            case ".wav" -> MediaType.parse("audio/wav");
            case ".m4a" -> MediaType.parse("audio/m4a");
            case ".webm" -> MediaType.parse("audio/webm");
            case ".ogg" -> MediaType.parse("audio/ogg");
            case ".flac" -> MediaType.parse("audio/flac");
            default -> MediaType.parse("application/octet-stream");
        };
    }
}
