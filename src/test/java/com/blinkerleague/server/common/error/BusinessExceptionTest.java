package com.blinkerleague.server.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

	@Test
	void 메시지_없이_만들면_ErrorCode의_기본_메시지를_쓴다() {
		BusinessException e = new BusinessException(ErrorCode.FORBIDDEN);

		assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
		assertThat(e.getMessage()).isEqualTo("접근 권한이 없습니다.");
	}

	@Test
	void 메시지를_주면_그_메시지를_쓰고_ErrorCode는_유지한다() {
		BusinessException e = new BusinessException(ErrorCode.CONFLICT, "이미 처리된 운행입니다.");

		assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
		assertThat(e.getMessage()).isEqualTo("이미 처리된 운행입니다.");
	}
}
