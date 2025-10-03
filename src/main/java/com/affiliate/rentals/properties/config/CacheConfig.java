package com.affiliate.rentals.properties.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Para producción configura un cache manager (RedisCacheManager) en vez de cache simple
}