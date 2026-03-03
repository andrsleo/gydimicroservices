package com.affiliate.rentals.gydi.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Profile("prod")
public class AwsSesConfig {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Bean
    public SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
        return SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider) // Provided automatically by spring-cloud-aws
                .build();
    }
}
