package com.blinkerleague.server.common.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 본문 크기 상한(D16). 상한을 넘는 요청은 본문을 읽기 전에 413 {@code PAYLOAD_TOO_LARGE}로 거부한다.
 * 인증보다 먼저 돌아 큰 본문을 받지 않는다.
 */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

	private final long maxBytes;
	private final ObjectMapper objectMapper;

	public RequestBodyLimitFilter(DataSize maxBodySize, ObjectMapper objectMapper) {
		this.maxBytes = maxBodySize.toBytes();
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		if (request.getContentLengthLong() > maxBytes) {
			reject(response);
			return;
		}
		chain.doFilter(request, response);
	}

	private void reject(HttpServletResponse response) throws IOException {
		ErrorCode errorCode = ErrorCode.PAYLOAD_TOO_LARGE;
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
	}
}
