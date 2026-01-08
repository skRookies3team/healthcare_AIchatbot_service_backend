package com.petlog.healthcare.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 Configuration
 *
 * User Service와 동일한 S3 버킷 사용
 * 환경변수: AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_S3_BUCKET, AWS_BUCKET_REGION
 *
 * @author healthcare-team
 * @since 2026-01-07
 */
@Slf4j
@Configuration
public class S3Config {

    // User Service와 동일한 변수명 사용
    @Value("${AWS_ACCESS_KEY:}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY:}")
    private String secretKey;

    @Value("${AWS_BUCKET_REGION:ap-northeast-2}")
    private String region;

    @Value("${AWS_S3_BUCKET:}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        log.info("═══════════════════════════════════════");
        log.info("🗂️ S3 Client 설정 (User Service 공통 버킷)");
        log.info("   Region: {}", region);
        log.info("   Bucket: {}", bucketName);
        log.info("═══════════════════════════════════════");

        if (accessKey == null || accessKey.isEmpty() ||
                secretKey == null || secretKey.isEmpty()) {
            log.warn("⚠️ AWS S3 자격 증명 미설정 - S3 기능 비활성화");
            return null;
        }

        if (bucketName == null || bucketName.isEmpty()) {
            log.warn("⚠️ AWS_S3_BUCKET 미설정 - S3 기능 비활성화");
            return null;
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public S3Properties s3Properties() {
        return new S3Properties(bucketName, region);
    }

    @Getter
    public static class S3Properties {
        private final String bucketName;
        private final String region;

        public S3Properties(String bucketName, String region) {
            this.bucketName = bucketName;
            this.region = region;
        }
    }
}
