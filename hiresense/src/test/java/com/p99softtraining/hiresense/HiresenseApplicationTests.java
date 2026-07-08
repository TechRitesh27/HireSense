package com.p99softtraining.hiresense;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.ai.google.genai.api-key=test-key-not-real"
})
class HiresenseApplicationTests {

	@Configuration
	static class TestConfig {
		@Bean
		ChatClient chatClient() {
			return Mockito.mock(ChatClient.class);
		}
	}

	@Test
	void contextLoads() {
	}

}
