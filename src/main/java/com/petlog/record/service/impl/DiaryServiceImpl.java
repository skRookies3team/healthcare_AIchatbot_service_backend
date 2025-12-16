package com.petlog.record.service.impl;

import com.petlog.record.client.PetServiceClient;
import com.petlog.record.client.StorageServiceClient;
import com.petlog.record.client.UserServiceClient;
import com.petlog.record.dto.request.DiaryRequest;
import com.petlog.record.dto.response.AiDiaryResponse; // AI 응답 DTO (패키지 경로 확인 필요)
import com.petlog.record.dto.response.DiaryResponse;
import com.petlog.record.entity.Diary;
import com.petlog.record.entity.DiaryImage;
import com.petlog.record.entity.ImageSource;
import com.petlog.record.entity.Visibility;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.DiaryRepository;

import com.petlog.record.service.DiaryService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;

    // MSA Clients
    private final UserServiceClient userClient;
    private final PetServiceClient petClient;

    @Qualifier("mockStorageServiceClient")
    private final StorageServiceClient storageServiceClient;

    // AI Components
    private final ChatModel chatModel;

    @Value("classpath:prompts/diary-system.st")
    private Resource systemPromptResource;

    /**
     * AI 일기 생성 로직
     */
    @Override
    @Transactional
    public Long createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility) {
        log.info("AI Diary creation started for user: {}, pet: {}", userId, petId);

        // 1. [검증] MSA 회원/펫 서비스 연동 (기존 로직 재사용)
        validateUserAndPet(userId, petId);

        // 2. [TODO] 실제 환경에서는 여기서 S3 등 스토리지에 이미지를 업로드하고 URL을 받아와야 합니다.
        // 현재는 임시 URL로 대체합니다.
        String uploadedImageUrl = "https://temporary-url/test-image.jpg";
        // 예: String uploadedImageUrl = s3Uploader.upload(imageFile);

        // 3. AI 모델 호출 및 텍스트 생성
        AiDiaryResponse aiResponse = generateContentWithAi(imageFile);

        // 4. Entity 생성 및 저장
        Diary diary = Diary.builder()
                .userId(userId)
                .petId(petId)
                .content(aiResponse.getContent()) // AI가 생성한 내용
                .mood(aiResponse.getMood())       // AI가 분석한 기분
                .weather("맑음") // 날씨는 별도 API나 입력이 없다면 기본값 또는 AI 추론 추가 가능
                .isAiGen(true)
                .visibility(visibility)
                .build();

        DiaryImage diaryImage = DiaryImage.builder()
                .imageUrl(uploadedImageUrl)
                .userId(userId)
                .imgOrder(1)
                .mainImage(true)
                .source(ImageSource.GALLERY)
                .build();

        diary.addImage(diaryImage);

        Diary savedDiary = diaryRepository.save(diary);

        // 5. 보관함 전송 (기존 로직 재사용)
        processDiaryImages(savedDiary);

        return savedDiary.getDiaryId();
    }

    /**
     * AI 생성 내부 로직 분리
     */
    private AiDiaryResponse generateContentWithAi(MultipartFile imageFile) {
        // 응답 컨버터
        BeanOutputConverter<AiDiaryResponse> converter = new BeanOutputConverter<>(AiDiaryResponse.class);

        // 프롬프트 로딩
        String systemPromptText = new PromptTemplate(systemPromptResource).render();

        // 최종 프롬프트: 시스템 메시지 + 포맷 가이드
        String promptText = systemPromptText + "\n\n" + converter.getFormat();

        try {
            Media imageMedia = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageFile.getBytes()));
            UserMessage userMessage = new UserMessage(promptText, List.of(imageMedia));

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withTemperature(0.7) // 창의성 조절
                    .withModel("gpt-4o")  // 모델 설정
                    .build();

            Prompt prompt = new Prompt(userMessage, options);
            String responseContent = chatModel.call(prompt).getResult().getOutput().getContent();

            return converter.convert(responseContent);

        } catch (IOException e) {
            log.error("Image processing failed during AI generation", e);
            throw new RuntimeException("AI 생성 중 이미지 처리 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 및 펫 존재 여부 검증 (Extract Method)
     */
    private void validateUserAndPet(Long userId, Long petId) {
        // [1-1] 사용자 검증
        try {
            userClient.getUserInfo(userId);
            log.info("User validated: {}", userId);
        } catch (FeignException e) {
            log.warn("User validation failed for userId: {}", userId);
            throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        // [1-2] 펫 검증
        try {
            petClient.getPetInfo(petId);
            log.info("Pet validated: {}", petId);
        } catch (FeignException e) {
            log.warn("Pet validation failed for petId: {}", petId);
            throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND);
        }
    }

    /**
     * 이미지 보관함 전송 로직 (기존 로직 유지)
     */
    private void processDiaryImages(Diary diary) {
        List<DiaryImage> images = diary.getImages();
        if (images == null || images.isEmpty()) return;

        List<StorageServiceClient.PhotoRequest> newPhotos = images.stream()
                .filter(img -> img.getSource() == ImageSource.GALLERY)
                .map(img -> new StorageServiceClient.PhotoRequest(
                        img.getUserId(),
                        img.getImageUrl()
                ))
                .collect(Collectors.toList());

        if (!newPhotos.isEmpty()) {
            try {
                storageServiceClient.savePhotos(newPhotos);
                log.info("Storage Service: Transferred {} photos", newPhotos.size());
            } catch (Exception e) {
                log.warn("Storage Service Transfer Failed: {}", e.getMessage());
            }
        }
    }

    // --- 기존 CRUD 메서드 (createDiary 제외 또는 유지) ---

    @Override
    @Transactional
    public Long createDiary(DiaryRequest.Create request) {
        // 기존 수동 생성 로직 (필요하다면 유지)
        validateUserAndPet(request.getUserId(), request.getPetId());
        Diary diary = request.toEntity();
        Diary savedDiary = diaryRepository.save(diary);
        processDiaryImages(diary);
        return savedDiary.getDiaryId();
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        return DiaryResponse.fromEntity(diary);
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        diary.update(
                request.getContent() != null ? request.getContent() : diary.getContent(),
                request.getVisibility() != null ? request.getVisibility() : diary.getVisibility(),
                request.getWeather() != null ? request.getWeather() : diary.getWeather(),
                request.getMood() != null ? request.getMood() : diary.getMood()
        );
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));
        diaryRepository.delete(diary);
    }
}