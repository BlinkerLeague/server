package com.blinkerleague.server.common.web;

import java.io.IOException;

/** 길이를 미리 알 수 없는(chunked) 요청 본문이 읽는 도중 상한을 넘었다(D16). */
public class RequestBodyTooLargeException extends IOException {

	public RequestBodyTooLargeException(long maxBytes) {
		super("요청 본문이 " + maxBytes + "바이트를 넘었습니다.");
	}
}
