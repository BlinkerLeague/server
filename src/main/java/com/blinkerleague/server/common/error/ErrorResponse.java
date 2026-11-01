package com.blinkerleague.server.common.error;

/** 모든 API 오류 응답의 형식: {@code { "code": "...", "message": "..." }} */
public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
