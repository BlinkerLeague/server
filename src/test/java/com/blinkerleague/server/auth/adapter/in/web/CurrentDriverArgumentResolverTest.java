package com.blinkerleague.server.auth.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.blinkerleague.server.support.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CurrentDriverArgumentResolverTest.MeController.class)
class CurrentDriverArgumentResolverTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void CurrentDriver_인자에는_액세스_토큰의_driverId가_들어온다() throws Exception {
		mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.accessToken(42L)))
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
