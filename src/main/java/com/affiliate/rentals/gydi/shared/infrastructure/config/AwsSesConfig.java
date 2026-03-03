package com.affiliate.rentals.gydi.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@Profile("prod")
public class AwsSesConfig {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Bean
    public SesV2Client sesV2Client(AwsCredentialsProvider credentialsProvider) {
        return SesV2Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider)
                // Explicitly use URL Connection HTTP client to avoid Apache HttpClient
                // dependency
                // which causes NoClassDefFoundError (DefaultClientConnectionReuseStrategy) in
                // Railway
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}
