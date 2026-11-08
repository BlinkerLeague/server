package com.blinkerleague.server.common.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** local 프로필처럼 API 문서를 켠 상태. */
@SpringBootTest(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocsTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 문서를_켜면_인증_없이_OpenAPI_명세를_받는다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.paths['/api/trips'].post").exists());
	}

	@Test
	void 토큰에서_꺼내는_운전자_ID는_요청_파라미터로_문서에_나오지_않는다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/trips'].post.parameters").doesNotExist());
	}

	@Test
	void 기본은_Bearer_JWT_인증이고_공개_API는_인증이_없다고_표시한다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
			.andExpect(jsonPath("$.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/api/drivers'].post.security").isEmpty())
			.andExpect(jsonPath("$.paths['/api/auth/refresh'].post.security").isEmpty())
			.andExpect(jsonPath("$.paths['/api/config/thresholds'].get.security").isEmpty())
			.andExpect(jsonPath("$.paths['/api/trips'].post.security").doesNotExist());
	}

	@Test
	void 문서_제목과_인증_오류_형식_안내를_담는다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("깜빡이리그 서버 API"))
			.andExpect(jsonPath("$.info.description").value(org.hamcrest.Matchers.allOf(
				org.hamcrest.Matchers.containsString("Authorization: Bearer"),
				org.hamcrest.Matchers.containsString("{ \"code\", \"message\" }"))));
	}

	private static final String ERROR_SCHEMA = "#/components/schemas/ErrorResponse";

	@ParameterizedTest(name = "{0} {1} → {2}")
	@CsvSource({
		"/api/drivers,         post, 201, 400 409",
		"/api/auth/refresh,    post, 200, 400 401",
		"/api/trips,           post, 201 200, 400 401 409",
		"/api/trips,           get,  200, 400 401",
		"/api/trips/{tripId}/score, get, 200, 401 404",
		"/api/missions/today,  get,  200, 401 404",
		"/api/points,          get,  200, 401",
		"/api/streak,          get,  200, 401",
		"/api/badges,          get,  200, 401"})
	void 성공_응답과_오류_응답_코드를_ErrorResponse_형식으로_문서에_담는다(String path, String method,
		String successCodes, String errorCodes) throws Exception {
		var result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		String responses = "$.paths['" + path + "']." + method + ".responses";
		for (String code : successCodes.split(" ")) {
			result.andExpect(jsonPath(responses + "['" + code + "']").exists());
		}
		for (String code : errorCodes.split(" ")) {
			result.andExpect(jsonPath(responses + "['" + code + "'].content['application/json'].schema['$ref']")
				.value(ERROR_SCHEMA));
		}
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource({
		"/api/drivers,           post, 운전자 등록",
		"/api/auth/refresh,      post, 토큰 재발급",
		"/api/config/thresholds, get,  판정 기준값 조회",
		"/api/trips,             post, 운행 · 이벤트 업로드",
		"/api/trips,             get,  최근 경기 목록",
		"/api/trips/{tripId}/score, get, 운행 점수 조회",
		"/api/missions/today,    get,  오늘의 미션 조회",
		"/api/points,            get,  내 포인트 조회",
		"/api/streak,            get,  연속 달성 조회",
		"/api/badges,            get,  배지 진열장 조회"})
	void API마다_요약을_담는다(String path, String method, String summary) throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['" + path + "']." + method + ".summary").value(summary));
	}

	@ParameterizedTest(name = "{0} {1} → {2}")
	@CsvSource({
		"/api/drivers,           post, 운전자",
		"/api/auth/refresh,      post, 인증",
		"/api/config/thresholds, get,  설정",
		"/api/trips,             post, 운행",
		"/api/trips,             get,  운행",
		"/api/trips/{tripId}/score, get, 운행",
		"/api/missions/today,    get,  미션",
		"/api/points,            get,  포인트",
		"/api/streak,            get,  미션",
		"/api/badges,            get,  배지"})
	void API를_기능별_한글_태그로_묶는다(String path, String method, String tag) throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['" + path + "']." + method + ".tags[0]").value(tag));
	}

	@Test
	void 성공_응답은_JSON_형식으로_표시한다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/trips'].post.responses['201'].content['application/json'].schema['$ref']")
				.value("#/components/schemas/TripResult"))
			.andExpect(jsonPath("$.paths['/api/drivers'].post.responses['201'].content['application/json']").exists());
	}

	@Test
	void 이벤트_payload는_자유_형식_객체이고_TURN의_signalOn을_안내한다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.schemas.EventRequest.properties.payload.type").value("object"))
			.andExpect(jsonPath("$.components.schemas.EventRequest.properties.payload.description")
				.value(org.hamcrest.Matchers.containsString("signalOn")))
			.andExpect(jsonPath("$.components.schemas.JsonNode").doesNotExist());
	}
}
