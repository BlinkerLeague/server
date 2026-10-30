package com.blinkerleague.server.common.error;

import lombok.Getter;

/** 도메인 규칙 위반을 알리는 예외. {@link GlobalExceptionHandler}가 {@link ErrorCode}의 상태 코드로 응답한다. */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getDefaultMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
