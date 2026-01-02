package com.petlog.healthcare.infrastructure.kafka;

import com.petlog.healthcare.dto.event.DiaryEventMessage;
import com.petlog.healthcare.service.DiaryVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * ✅ Diary Service로부터 Kafka 이벤트 수신
 *
 * Topic: diary-events (Diary Service 8087 포트에서 발행)
 * Consumer Group: healthcare-group
 *
 * @author healthcare-team
 * @since 2025-12-23
 * @version 2.0 (Diary Service 연동 완료)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryEventConsumer {

    private final DiaryVectorService diaryVectorService;

    /**
     * ✅ Diary 이벤트 수신 및 처리
     *
     * Diary Service가 발행하는 이벤트:
     * - DIARY_CREATED: 일기 생성 → Milvus 벡터 저장
     * - DIARY_UPDATED: 일기 수정 → 벡터 업데이트
     * - DIARY_DELETED: 일기 삭제 → 벡터 삭제
     */
    @KafkaListener(
            topics = "diary-events",
            groupId = "healthcare-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload DiaryEventMessage event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack
    ) {
        log.info("═══════════════════════════════════════");
        log.info("📩 Kafka 메시지 수신");
        log.info("   Event Type: {}", event.getEventType());
        log.info("   Diary ID: {}", event.getDiaryId());
        log.info("   User ID: {}", event.getUserId());
        log.info("   Pet ID: {}", event.getPetId());
        log.info("   Partition: {}, Offset: {}", partition, offset);
        log.info("═══════════════════════════════════════");

        try {
            switch (event.getEventType()) {
                case "DIARY_CREATED" -> {
                    log.info("🆕 일기 생성 이벤트 처리 시작");

                    // ✅ DiaryVectorService.vectorizeAndStore 호출
                    diaryVectorService.vectorizeAndStore(
                            event.getDiaryId(),
                            event.getUserId(),
                            event.getPetId(),
                            event.getContent(),
                            event.getImageUrl(),
                            event.getCreatedAt()
                    );

                    log.info("✅ 일기 생성 이벤트 처리 완료");
                }

                case "DIARY_UPDATED" -> {
                    log.info("✏️ 일기 수정 이벤트 처리 시작");

                    // 기존 벡터 삭제 후 재생성
                    diaryVectorService.deleteVector(event.getDiaryId());
                    diaryVectorService.vectorizeAndStore(
                            event.getDiaryId(),
                            event.getUserId(),
                            event.getPetId(),
                            event.getContent(),
                            event.getImageUrl(),
                            event.getCreatedAt()
                    );

                    log.info("✅ 일기 수정 이벤트 처리 완료");
                }

                case "DIARY_DELETED" -> {
                    log.info("🗑️ 일기 삭제 이벤트 처리 시작");

                    diaryVectorService.deleteVector(event.getDiaryId());

                    log.info("✅ 일기 삭제 이벤트 처리 완료");
                }

                default -> {
                    log.warn("⚠️ 알 수 없는 이벤트 타입: {}", event.getEventType());
                }
            }

            // ✅ 수동 커밋 (처리 성공 시)
            if (ack != null) {
                ack.acknowledge();
                log.debug("✅ Kafka offset 커밋 완료");
            }

        } catch (Exception e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ 이벤트 처리 실패");
            log.error("   Diary ID: {}", event.getDiaryId());
            log.error("   Error: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════");

            // TODO: 실패한 이벤트는 Dead Letter Queue(DLQ)로 전송
            // 또는 재시도 로직 구현
        }
    }
}