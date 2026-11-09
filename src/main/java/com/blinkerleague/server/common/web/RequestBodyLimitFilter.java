package com.blinkerleague.server.common.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
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
		long contentLength = request.getContentLengthLong();
		if (contentLength > maxBytes) {
			reject(response);
			return;
		}
		// 길이를 모르는(chunked) 본문은 읽으면서 센다. 넘으면 RequestBodyTooLargeException → 413.
		chain.doFilter(contentLength < 0 ? new LimitedRequest(request, maxBytes) : request, response);
	}

	private static final class LimitedRequest extends HttpServletRequestWrapper {

		private final long maxBytes;
		private ServletInputStream limited;

		LimitedRequest(HttpServletRequest request, long maxBytes) {
			super(request);
			this.maxBytes = maxBytes;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (limited == null) {
				limited = new LimitedInputStream(super.getInputStream(), maxBytes);
			}
			return limited;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return new BufferedReader(new InputStreamReader(getInputStream(),
				getCharacterEncoding() == null ? StandardCharsets.UTF_8.name() : getCharacterEncoding()));
		}
	}

	private static final class LimitedInputStream extends ServletInputStream {

		private final ServletInputStream delegate;
		private final long maxBytes;
		private long read;

		LimitedInputStream(ServletInputStream delegate, long maxBytes) {
			this.delegate = delegate;
			this.maxBytes = maxBytes;
		}

		@Override
		public int read() throws IOException {
			int b = delegate.read();
			if (b != -1) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			int n = delegate.read(buffer, offset, length);
			if (n > 0) {
				count(n);
			}
			return n;
		}

		private void count(int n) throws RequestBodyTooLargeException {
			read += n;
			if (read > maxBytes) {
				throw new RequestBodyTooLargeException(maxBytes);
			}
		}

		@Override
		public boolean isFinished() {
			return delegate.isFinished();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setReadListener(ReadListener listener) {
			delegate.setReadListener(listener);
		}
	}

	private void reject(HttpServletResponse response) throws IOException {
		ErrorCode errorCode = ErrorCode.PAYLOAD_TOO_LARGE;
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
	}
}
