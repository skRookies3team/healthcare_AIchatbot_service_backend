package com.petlog.record.service;

import com.petlog.record.entity.Diary;
import com.petlog.record.entity.Visibility;
import org.springframework.web.multipart.MultipartFile;

public interface AiDiaryService {

    /**
     * AI를 이용한 자동 일기 생성
     * * @param userId 사용자 ID
     * @param petId 펫 ID
     * @param imageFile 업로드할 이미지 파일
     * @param visibility 공개 범위
     * @return 생성된 Diary 엔티티
     */
    Diary createAiDiary(Long userId, Long petId, MultipartFile imageFile, Visibility visibility);
}