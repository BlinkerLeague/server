package com.blinkerleague.server.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.blinkerleague.server.common.web.RequestBodyTooLargeException;

/** 스프링 컨텍스트 없이 핸들러 메서드를 직접 호출해 상태 코드와 응답 본문을 확인한다. */
class GlobalExceptionHandlerUnitTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void BusinessException은_ErrorCode의_상태와_예외_메시지로_응답한다() {
		ResponseEntity<ErrorResponse> response =
			handler.handleBusiness(new BusinessException(ErrorCode.CONFLICT, "이미 처리된 운행입니다."));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse("CONFLICT", "이미 처리된 운행입니다."));
	}

	@Test
	void 검증_실패는_400과_모든_필드_오류를_쉼표로_이어_응답한다() throws Exception {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "name", "공백일 수 없습니다"));
		bindingResult.addError(new FieldError("request", "age", "0 이상이어야 합니다"));
		MethodParameter parameter = new MethodParameter(
			GlobalExceptionHandlerUnitTest.class.getDeclaredMethod("target", Object.class), 0);

		ResponseEntity<ErrorResponse> response =
			handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody())
			.isEqualTo(new ErrorResponse("INVALID_INPUT", "name: 공백일 수 없습니다, age: 0 이상이어야 합니다"));
	}

	@Test
	void 읽을_수_없는_요청은_400과_기본_메시지로_응답한다() {
		ResponseEntity<ErrorResponse> response = handler.handleUnreadable(new IllegalArgumentException("bad"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INVALID_INPUT));
	}

	@Test
	void 본문을_읽다가_크기_상한을_넘으면_413_PAYLOAD_TOO_LARGE로_응답한다() {
		HttpMessageNotReadableException e = new HttpMessageNotReadableException("I/O error",
			new RequestBodyTooLargeException(1024), new MockHttpInputMessage(new byte[0]));

		ResponseEntity<ErrorResponse> response = handler.handleUnreadable(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.PAYLOAD_TOO_LARGE));
	}

	@Test
	void 없는_경로는_404로_응답한다() {
		ResponseEntity<ErrorResponse> response =
			handler.handleNoResource(new NoResourceFoundException(HttpMethod.GET, "/api/unknown"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.NOT_FOUND));
	}

	@Test
	void 지원하지_않는_메서드는_405로_응답한다() {
		ResponseEntity<ErrorResponse> response =
			handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("DELETE"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
	}

	@Test
	void 예상하지_못한_예외는_500과_내부_메시지를_숨긴_기본_메시지로_응답한다() {
		ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("DB 비밀번호 노출"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}

	@SuppressWarnings("unused")
	private void target(Object request) {
	}
}
