package com.petlog.healthcare.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 설정
 *
 * JSON 직렬화/역직렬화 설정
 *
 * WHY 별도 설정?
 * - LocalDateTime 처리 (JavaTimeModule)
 * - 날짜 형식 통일 (ISO-8601)
 * - Pretty Print (개발 환경)
 *
 * @author healthcare-team
 * @since 2025-12-19
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper Bean 생성
     *
     * @return ObjectMapper - JSON 파싱용
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 날짜/시간 지원
        mapper.registerModule(new JavaTimeModule());

        // 날짜를 타임스탬프가 아닌 ISO-8601 형식으로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
