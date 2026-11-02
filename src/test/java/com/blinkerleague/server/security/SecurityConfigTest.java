package com.blinkerleague.server.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.security.jwt.JwtProperties;
import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 유효한_액세스_토큰이면_인증을_통과한다() throws Exception {
		// 컨트롤러가 아직 없으므로 인증을 통과해 404가 나오면 성공이다.
		mockMvc.perform(get("/api/trips/some-trip/score")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(1L)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 다른_secret으로_서명한_토큰이면_401_INVALID_TOKEN을_반환한다() throws Exception {
		JwtProperties forger = new JwtProperties(
			"attacker-secret-key-that-is-also-32bytes-long!", Duration.ofHours(1), Duration.ofDays(30));

		mockMvc.perform(get("/api/trips/some-trip/score")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(forger, 1L, Clock.systemUTC())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void 만료된_토큰이면_401_INVALID_TOKEN을_반환한다() throws Exception {
		Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);

		mockMvc.perform(get("/api/trips/some-trip/score")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(TestTokens.JWT, 1L, twoHoursAgo)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void 인증없이_보호된_API를_호출하면_401_JSON을_반환한다() throws Exception {
		mockMvc.perform(get("/api/trips/some-trip/score"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 운전자_등록은_인증없이_통과한다() throws Exception {
		// 토큰 없이 본문 없는 요청을 보내 401이 아니라 요청 검증(400)까지 도달하면 허용된 것이다.
		mockMvc.perform(post("/api/drivers"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 설정_조회는_인증없이_통과한다() throws Exception {
		mockMvc.perform(get("/api/config/thresholds"))
			.andExpect(status().isOk());
	}
}
