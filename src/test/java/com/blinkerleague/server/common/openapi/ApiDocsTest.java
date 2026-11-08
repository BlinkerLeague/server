package com.blinkerleague.server.common.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
