package com.gnilc.novum.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(S3Properties.class)
public class S3Configuration {

    @Bean
    @ConditionalOnMissingBean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    S3Client s3Client(S3Properties properties) {
        properties.validate();
        return S3Client.builder()
                .endpointOverride(properties.getEndpoint())
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(pathStyleConfiguration())
                .build();
    }

    @Bean
    S3Presigner s3Presigner(S3Properties properties) {
        properties.validate();
        return S3Presigner.builder()
                .endpointOverride(properties.getEndpoint())
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(pathStyleConfiguration())
                .build();
    }

    @Bean
    S3Service s3Service(
            S3Client client,
            S3Presigner presigner,
            S3Properties properties,
            Clock clock) {
        return new DefaultS3Service(client, presigner, properties, clock);
    }

    private static StaticCredentialsProvider credentials(S3Properties properties) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                properties.getAccessKey(), properties.getSecretKey()));
    }

    private static software.amazon.awssdk.services.s3.S3Configuration pathStyleConfiguration() {
        return software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }
}
