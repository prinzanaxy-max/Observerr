package com.backend.observerr.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String STUDENT_RESULTS_PAGE_CACHE = "studentResultsPage";
    public static final String STUDENT_RESULTS_SUMMARY_CACHE = "studentResultsSummary";

    @Bean
    CacheManager cacheManager(
            @Value("${cache.student-results.ttl-seconds:300}") long ttlSeconds,
            @Value("${cache.student-results.max-size:500}") long maxSize) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                STUDENT_RESULTS_PAGE_CACHE,
                STUDENT_RESULTS_SUMMARY_CACHE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize));
        return cacheManager;
    }
}
