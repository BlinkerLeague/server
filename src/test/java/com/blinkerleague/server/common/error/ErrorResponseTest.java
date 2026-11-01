package com.blinkerleague.server.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

	@Test
	void ErrorCode만_주면_코드_이름과_기본_메시지를_담는다() {
		ErrorResponse response = ErrorResponse.of(ErrorCode.NOT_FOUND);

		assertThat(response.code()).isEqualTo("NOT_FOUND");
		assertThat(response.message()).isEqualTo("대상을 찾을 수 없습니다.");
	}

	@Test
	void 메시지를_함께_주면_기본_메시지_대신_그_메시지를_담는다() {
		ErrorResponse response = ErrorResponse.of(ErrorCode.CONFLICT, "이미 처리된 운행입니다.");

		assertThat(response.code()).isEqualTo("CONFLICT");
		assertThat(response.message()).isEqualTo("이미 처리된 운행입니다.");
	}
}
