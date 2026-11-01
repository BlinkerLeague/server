package com.blinkerleague.server.driver.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.support.RedisContainerTest;

/** 실제 Redis(Testcontainers)를 쓰는 운전자 등록 API 통합 테스트. Docker가 필요하다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverControllerTest extends RedisContainerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 운전자를_등록하면_201과_운전자_정보_액세스_리프레시_토큰을_준다() throws Exception {
		mockMvc.perform(post("/api/drivers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"깜빡왕\",\"regionCode\":\"11680\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.driverId").isNumber())
			.andExpect(jsonPath("$.nickname").value("깜빡왕"))
			.andExpect(jsonPath("$.regionCode").value("11680"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty());
	}
}
