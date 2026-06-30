package com.p99softtraining.hiresense.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableCaching
public class CacheConfig {

    // Cache name constants — used in annotations across service layer
    public static final String COMPANIES        = "companies";
    public static final String HIRING_DRIVES    = "hiringDrives";
    public static final String ROUNDS           = "rounds";
    public static final String INTERVIEWERS     = "interviewers";
    public static final String CANDIDATE_PROFILE = "candidateProfile";
    public static final String QUESTIONS        = "questions";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                COMPANIES,
                HIRING_DRIVES,
                ROUNDS,
                INTERVIEWERS,
                CANDIDATE_PROFILE,
                QUESTIONS
        );
    }
}
