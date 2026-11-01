package com.blinkerleague.server.auth.adapter.in.web;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.blinkerleague.server.auth.application.service.RefreshTokenService;

/** 실제 Redis(Testcontainers)를 쓰는 재발급 API 통합 테스트. Docker가 필요하다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthControllerTest {

	@Container
	private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RefreshTokenService refreshTokenService;

	@Test
	void 리프레시_토큰으로_재발급을_요청하면_200과_새_액세스_토큰_새_리프레시_토큰을_준다() throws Exception {
		String refreshToken = refreshTokenService.issue(7L);

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").value(not(refreshToken)));
	}
}
