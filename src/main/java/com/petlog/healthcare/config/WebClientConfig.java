package com.petlog.healthcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정
 *
 * WHY WebClient?
 * - 비동기 HTTP 요청 (네이버 API, 크롤링)
 * - RestTemplate보다 성능 좋음
 * - Spring WebFlux 기반
 *
 * @author healthcare-team
 * @since 2025-12-31
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient Bean 생성
     *
     * WHY maxInMemorySize?
     * - 기본값 256KB → 2MB로 증가
     * - 큰 HTML 페이지 크롤링 대비
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024) // 2MB
                )
                .build();
    }
}