package com.blinkerleague.server.common.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

class RequestBodyLimitFilterTest {

	private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter(DataSize.ofBytes(10), new ObjectMapper());

	@Test
	void 길이를_모르는_요청_본문도_상한을_넘게_읽으면_RequestBodyTooLargeException이_난다() {
		MockHttpServletRequest request = new ChunkedRequest("x".repeat(11).getBytes());
		FilterChain readAll = (req, res) -> req.getInputStream().readAllBytes();

		assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), readAll))
			.isInstanceOf(RequestBodyTooLargeException.class);
	}

	// Transfer-Encoding: chunked처럼 Content-Length가 없는 요청
	private static class ChunkedRequest extends MockHttpServletRequest {

		ChunkedRequest(byte[] body) {
			super("POST", "/api/trips");
			setContent(body);
		}

		@Override
		public int getContentLength() {
			return -1;
		}

		@Override
		public long getContentLengthLong() {
			return -1;
		}
	}
}
