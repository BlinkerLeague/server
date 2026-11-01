package com.blinkerleague.server.auth.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.out.jwt.JwtAccessTokenIssuer;
import com.blinkerleague.server.security.jwt.JwtConfig;
import com.blinkerleague.server.security.jwt.JwtProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CurrentDriverArgumentResolverTest.MeController.class)
class CurrentDriverArgumentResolverTest {

	// application-test.yaml의 jwt 설정과 같은 값
	private static final JwtProperties JWT = new JwtProperties(
		"blinkerleague-test-secret-key-32bytes-or-more!", Duration.ofHours(1), Duration.ofDays(30));

	@Autowired
	MockMvc mockMvc;

	@Test
	void CurrentDriver_인자에는_액세스_토큰의_driverId가_들어온다() throws Exception {
		String token = new JwtAccessTokenIssuer(new JwtConfig().jwtEncoder(JWT), JWT, Clock.systemUTC())
			.issueAccessToken(42L);

		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(content().string("42"));
	}

	@RestController
	static class MeController {

		@GetMapping("/test/me")
		Long me(@CurrentDriver Long driverId) {
			return driverId;
		}
	}
}
