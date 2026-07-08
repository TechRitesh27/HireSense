package com.p99softtraining.hiresense.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Builds a ChatClient from the auto-configured ChatClient.Builder.
     * Spring AI provides ChatClient.Builder as a bean; ChatClient itself
     * must be explicitly constructed and registered.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
