package com.affiliate.rentals.gydi.shared.config;

import com.affiliate.rentals.gydi.shared.util.SlugGenerator;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for URL slug generation.
 * Provides Hashids encoder for property IDs and SlugGenerator utility.
 */
@Configuration
public class SlugConfig {

    @Value("${security.url.hashids.salt}")
    private String hashidsSalt;

    @Value("${security.url.hashids.min-length:10}")
    private int hashidsMinLength;

    /**
     * Creates Hashids encoder for property ID obfuscation.
     *
     * @return Hashids instance configured with application salt
     */
    @Bean
    public Hashids hashids() {
        return new Hashids(hashidsSalt, hashidsMinLength);
    }

    /**
     * Creates SlugGenerator bean for SEO-friendly URL generation.
     *
     * @return SlugGenerator instance
     */
    @Bean
    public SlugGenerator slugGenerator() {
        return new SlugGenerator();
    }
}
