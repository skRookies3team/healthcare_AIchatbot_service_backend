package com.petlog.healthcare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

@Slf4j
@Configuration
public class BedrockConfig {

    @Value("${aws.bedrock.region:ap-northeast-2}")
    private String region;

    @Value("${aws.bedrock.api-key}")
    private String apiKey;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-haiku-20241022-v1:0}")
    private String modelId;

    /**
     * BedrockRuntimeClient + API Key HttpClient
     * 131자 API Key Header 자동 추가 (x-api-key)
     */
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        log.info("🔥 Bedrock API Key 클라이언트 초기화 - Region: {}", region);

        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(30))
                .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                .build();

        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .httpClient(httpClient)
                .build();
    }
}
