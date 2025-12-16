package com.petlog.record.service.impl;

import com.petlog.record.dto.response.AiDiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import com.petlog.record.repository.DiaryRepository;
import com.petlog.record.service.AiDiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiDiaryServiceImpl implements AiDiaryService {

    private final DiaryRepository diaryRepository;
    private final ChatModel chatModel;

    // 외부 리소스 파일(프롬프트) 주입
    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    @Override
    public Diary createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility) {
        log.info("AI Diary creation started for user: {}, pet: {}", userId, petId);

        String tempDbUrl = "https://temporary-url/test-image.jpg";

        // [1] 응답 컨버터 생성
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        // [2] 프롬프트 템플릿 로딩 및 텍스트 생성
        String systemPromptText = new PromptTemplate(systemPromptResource).render();

        // [3] 최종 프롬프트 합성: 시스템 프롬프트(파일) + 포맷 가이드(자동생성)
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageFile.getBytes()));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));

            // [4] 옵션 설정
            // [수정] 버전 호환성을 위해 withTemperature(double) 방식으로 변경합니다.
            // 만약 Gradle이 최신 버전(M4)을 완벽하게 인식했다면 .temperature(0.7)이 맞지만,
            // IDE 에러 해결을 위해 구버전/신버전 과도기 문법인 .with... 방식을 시도합니다.
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withTemperature(0.7) // double 타입 사용
                    .withModel("gpt-4o")
                    .build();

            // [5] 요청 전송
            Prompt prompt = new Prompt(userMessage, options);
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();

            // 응답 변환
            AiDiaryResponse aiResponse = converter.convert(responseContent);

            log.debug("AI Response received - Mood: {}", aiResponse.getMood());

            Diary diary = Diary.builder()
                    .userId(userId)
                    .petId(petId)
                    .content(aiResponse.getContent())
                    .mood(aiResponse.getMood())
                    .isAiGen(true)
                    .visibility(visibility)
                    .build();

            DiaryImage diaryImage = DiaryImage.builder()
                    .imageUrl(tempDbUrl)
                    .userId(userId)
                    .imgOrder(1)
                    .mainImage(true)
                    .source(ImageSource.GALLERY)
                    .build();

            diary.addImage(diaryImage);

            return diaryRepository.save(diary);

        } catch (IOException e) {
            log.error("Image processing failed", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }
}