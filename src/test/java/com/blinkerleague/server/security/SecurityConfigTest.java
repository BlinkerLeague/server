package com.blinkerleague.server.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.auth.adapter.out.jwt.JwtAccessTokenIssuer;
import com.blinkerleague.server.security.jwt.JwtConfig;
import com.blinkerleague.server.security.jwt.JwtProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

	// application-test.yaml의 jwt 설정과 같은 값
	private static final JwtProperties JWT = new JwtProperties(
		"blinkerleague-test-secret-key-32bytes-or-more!", Duration.ofHours(1), Duration.ofDays(30));

	@Autowired
	MockMvc mockMvc;

	@Test
	void 유효한_액세스_토큰이면_인증을_통과한다() throws Exception {
		// 컨트롤러가 아직 없으므로 인증을 통과해 404가 나오면 성공이다.
		mockMvc.perform(get("/api/trips/some-trip/score")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(JWT, Clock.systemUTC())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 인증없이_보호된_API를_호출하면_401_JSON을_반환한다() throws Exception {
		mockMvc.perform(get("/api/trips/some-trip/score"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 운전자_등록은_인증없이_통과한다() throws Exception {
		// 컨트롤러가 아직 없으므로 인증을 통과해 404가 나오면 허용된 것이다.
		mockMvc.perform(post("/api/drivers"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void 설정_조회는_인증없이_통과한다() throws Exception {
		mockMvc.perform(get("/api/config/thresholds"))
			.andExpect(status().isNotFound());
	}

	private static String accessToken(JwtProperties properties, Clock clock) {
		return new JwtAccessTokenIssuer(new JwtConfig().jwtEncoder(properties), properties, clock).issueAccessToken(1L);
	}
}
