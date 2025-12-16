package com.petlog.record.service.impl;

import com.petlog.record.client.NotificationServiceClient;
import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import com.petlog.record.entity.Recap;
import com.petlog.record.exception.EntityNotFoundException;
import com.petlog.record.exception.ErrorCode;
import com.petlog.record.repository.RecapRepository;
import com.petlog.record.service.RecapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final NotificationServiceClient notificationClient;

    @Override
    @Transactional
    public Long createRecap(RecapRequest.Create request) {
        // 1. DTO -> Entity 변환 (DTO 내부 로직 사용)
        Recap recap = request.toEntity();

        // 2. 저장 (Cascade 설정으로 인해 Highlight도 자동 저장됨)
        Recap savedRecap = recapRepository.save(recap);

        // 3. 알림 발송 (로컬 테스트 시 알림 서비스가 없어도 동작하도록 예외 처리)
        /*
        try {
            notificationClient.sendNotification(new NotificationServiceClient.NotificationRequest(
                    request.getUserId(),
                    "✨ 월간 리캡이 도착했습니다!",
                    savedRecap.getTitle() + "의 추억을 확인해보세요.",
                    "RECAP_CREATED"
            ));
        } catch (Exception e) {
            log.warn("알림 서비스 호출 실패: {}", e.getMessage());
        }
        */

        return savedRecap.getRecapId();
    }

    @Override
    public RecapResponse.Detail getRecap(Long recapId) {
        Recap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RECAP_NOT_FOUND));

        // 1. Entity -> DTO 변환 (메서드명 변경: from -> fromEntity)
        RecapResponse.Detail response = RecapResponse.Detail.fromEntity(recap);

        // 2. [TODO] 헬스케어 서비스 호출하여 건강 데이터 채우기 (추후 구현)
        /*
        try {
            // HealthReportDto healthData = healthClient.getReport(...);
            // if (healthData != null) {
            //      response.setAvgHeartRate(healthData.getHeartRate());
            //      ...
            // }
        } catch (Exception e) {
            log.warn("헬스케어 데이터 조회 실패 (리캡 상세 조회는 계속 진행): {}", e.getMessage());
        }
        */

        return response;
    }

    @Override
    public List<RecapResponse.Simple> getAllRecaps(Long userId) {
        return recapRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecapResponse.Simple::fromEntity) // 메서드명 변경: from -> fromEntity
                .collect(Collectors.toList());
    }

    @Override
    public List<RecapResponse.Simple> getRecapsByPet(Long petId) {
        return recapRepository.findAllByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(RecapResponse.Simple::fromEntity) // 메서드명 변경: from -> fromEntity
                .collect(Collectors.toList());
    }
}