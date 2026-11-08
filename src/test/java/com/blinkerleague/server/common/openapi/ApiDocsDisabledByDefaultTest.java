package com.blinkerleague.server.common.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 배포 환경에 API 문서를 노출하지 않는다(Pleos 보안 가이드). local 프로필만 켠다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocsDisabledByDefaultTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void 기본_설정에서는_API_문서와_Swagger_UI가_없다() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
	}
}
