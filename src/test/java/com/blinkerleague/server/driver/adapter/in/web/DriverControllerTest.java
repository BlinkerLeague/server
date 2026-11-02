package com.blinkerleague.server.driver.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.blinkerleague.server.support.RedisContainerTest;
import com.jayway.jsonpath.JsonPath;

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

	@ParameterizedTest
	@ValueSource(strings = {"", "깜", "가가가가가가가가가가가가가"})
	void 닉네임이_2자에서_12자가_아니면_400_INVALID_INPUT을_반환한다(String nickname) throws Exception {
		mockMvc.perform(post("/api/drivers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"" + nickname + "\",\"regionCode\":\"11680\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "1168", "116800", "1168a"})
	void 지역_코드가_5자리_숫자가_아니면_400_INVALID_INPUT을_반환한다(String regionCode) throws Exception {
		mockMvc.perform(post("/api/drivers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"지역검증\",\"regionCode\":\"" + regionCode + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 이미_사용_중인_닉네임으로_등록하면_409_CONFLICT를_반환한다() throws Exception {
		String body = "{\"nickname\":\"중복닉네임\",\"regionCode\":\"11680\"}";
		mockMvc.perform(post("/api/drivers").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/drivers").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void 등록으로_받은_액세스_토큰으로_보호_API를_통과하고_리프레시_토큰으로_재발급할_수_있다() throws Exception {
		String response = mockMvc.perform(post("/api/drivers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"흐름검증\",\"regionCode\":\"11680\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		String accessToken = JsonPath.read(response, "$.accessToken");
		String refreshToken = JsonPath.read(response, "$.refreshToken");

		// 해당 컨트롤러가 아직 없으므로 인증을 통과해 404가 나오면 성공이다.
		mockMvc.perform(get("/api/trips/some-trip/score").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
			.andExpect(status().isOk());
	}
}
