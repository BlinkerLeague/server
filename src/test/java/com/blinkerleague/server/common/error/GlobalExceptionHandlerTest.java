package com.blinkerleague.server.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerTest.ErrorTestController.class)
@WithMockUser
class GlobalExceptionHandlerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void BusinessException은_ErrorCode의_상태와_메시지로_응답한다() throws Exception {
		mockMvc.perform(get("/test/errors/business"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONFLICT"))
			.andExpect(jsonPath("$.message").value("이미 처리된 운행입니다."));
	}

	@Test
	void 검증_실패는_400과_필드_메시지로_응답한다() throws Exception {
		mockMvc.perform(post("/test/errors/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("name:")));
	}

	@Test
	void 읽을_수_없는_본문은_400으로_응답한다() throws Exception {
		mockMvc.perform(post("/test/errors/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 예상하지_못한_예외는_500으로_응답한다() throws Exception {
		mockMvc.perform(get("/test/errors/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
	}

	@RestController
	static class ErrorTestController {

		@GetMapping("/test/errors/business")
		void business() {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 운행입니다.");
		}

		@PostMapping("/test/errors/validation")
		void validation(@Valid @RequestBody NameRequest request) {
		}

		@GetMapping("/test/errors/unexpected")
		void unexpected() {
			throw new IllegalStateException("boom");
		}
	}

	record NameRequest(@NotBlank String name) {
	}
}
