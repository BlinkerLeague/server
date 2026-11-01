package com.blinkerleague.server.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

	@Autowired
	MockMvc mockMvc;

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
}
