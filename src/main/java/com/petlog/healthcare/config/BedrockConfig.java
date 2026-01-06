package com.petlog.healthcare.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

/**
 * AWS Bedrock 설정
 *
 * ⚠️ 필수 환경변수:
 * - AWS_ACCESS_KEY_ID: IAM 사용자의 Access Key ID (AKIA...)
 * - AWS_SECRET_ACCESS_KEY: IAM 사용자의 Secret Access Key
 * - AWS_BEDROCK_REGION: us-east-1 권장
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Configuration
public class BedrockConfig {

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String secretAccessKey;

    @Value("${AWS_BEDROCK_REGION:us-east-1}")
    private String region;

    @Value("${AWS_BEDROCK_MODEL_ID:anthropic.claude-3-5-sonnet-20240620-v1:0}")
    private String modelId;

    @Value("${AWS_BEDROCK_HAIKU_MODEL_ID:anthropic.claude-3-haiku-20240307-v1:0}")
    private String haikuModelId;

    @Value("${AWS_BEDROCK_MAX_TOKENS:2000}")
    private int maxTokens;

    @Bean
    public BedrockProperties bedrockProperties() {
        log.info("===========================================");
        log.info(" 🔥 Bedrock 설정");
        log.info("===========================================");
        log.info("   Region: {}", region);
        log.info("   Sonnet: {}", modelId);
        log.info("   Haiku: {}", haikuModelId);
        log.info("   Access Key ID: {}",
                accessKeyId != null && accessKeyId.length() > 8
                        ? accessKeyId.substring(0, 8) + "..."
                        : "❌ 미설정");
        log.info("   Secret Access Key: {}",
                secretAccessKey != null && !secretAccessKey.isEmpty()
                        ? "****설정됨****"
                        : "❌ 미설정");
        log.info("===========================================");

        if (accessKeyId == null || accessKeyId.isEmpty()) {
            log.error("❌ AWS_ACCESS_KEY_ID 환경변수를 설정하세요!");
            log.error("   IntelliJ: Run > Edit Configurations > Environment variables");
        }
        if (secretAccessKey == null || secretAccessKey.isEmpty()) {
            log.error("❌ AWS_SECRET_ACCESS_KEY 환경변수를 설정하세요!");
        }

        return new BedrockProperties(accessKeyId, region, modelId, haikuModelId, maxTokens);
    }

    @Getter
    public static class BedrockProperties {
        private final String apiKey;
        private final String region;
        private final String modelId;
        private final String haikuModelId;
        private final int maxTokens;

        public BedrockProperties(String apiKey, String region, String modelId,
                String haikuModelId, int maxTokens) {
            this.apiKey = apiKey;
            this.region = region;
            this.modelId = modelId;
            this.haikuModelId = haikuModelId;
            this.maxTokens = maxTokens;
        }
    }

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        log.info("� BedrockRuntimeClient 생성");

        if (accessKeyId == null || accessKeyId.isEmpty() ||
                secretAccessKey == null || secretAccessKey.isEmpty()) {
            log.error("⛔ AWS 자격 증명 미설정!");
            log.error("   다음 환경변수를 설정하세요:");
            log.error("   - AWS_ACCESS_KEY_ID=AKIA...");
            log.error("   - AWS_SECRET_ACCESS_KEY=...");
            throw new IllegalStateException("AWS 자격 증명이 설정되지 않았습니다.");
        }

        log.info("   ✅ Access Key ID: {}...", accessKeyId.substring(0, 8));
        log.info("   ✅ Region: {}", region);

        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }
}