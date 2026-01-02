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
 * Kafka Consumer: Diary 이벤트 처리
 * - enable-auto-commit: false → 수동 commit
 * - Acknowledgment ack로 처리 성공 후만 commit
 *
 * @author healthcare-team
 * @since 2026-01-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryEventConsumer {

    private final DiaryVectorService diaryVectorService;

    @KafkaListener(topics = "diary-events", groupId = "healthcare-group")
    public void consume(@Payload DiaryEventMessage event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {  // 🔥 추가!

        log.info("📩 Kafka 메시지 수신: diaryId={}, partition={}, offset={}",
                event.getDiaryId(), partition, offset);

        try {
            // 이벤트 처리
            switch (event.getEventType()) {
                case "DIARY_CREATED" -> {
                    log.info("🔄 DIARY_CREATED 처리: diaryId={}", event.getDiaryId());
                    diaryVectorService.vectorizeAndStore(event);
                }
                case "DIARY_UPDATED" -> {
                    log.info("🔄 DIARY_UPDATED 처리: diaryId={}", event.getDiaryId());
                    diaryVectorService.updateVector(event);
                }
                case "DIARY_DELETED" -> {
                    log.info("🔄 DIARY_DELETED 처리: diaryId={}", event.getDiaryId());
                    diaryVectorService.deleteVector(event.getDiaryId());
                }
                default -> log.warn("⚠️ 알 수 없는 이벤트: {}", event.getEventType());
            }

            // 🔥 처리 성공 후만 commit!
            ack.acknowledge();
            log.info("✅ Ack 완료: diaryId={}", event.getDiaryId());

        } catch (Exception e) {
            // 처리 실패 → Ack 미호출 → 재시도
            log.error("❌ 처리 실패 (재시도): diaryId={}, error={}",
                    event.getDiaryId(), e.getMessage());
            // ack.acknowledge() 하지 않음!
        }
    }
}
