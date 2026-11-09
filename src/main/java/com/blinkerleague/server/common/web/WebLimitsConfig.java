package com.blinkerleague.server.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.unit.DataSize;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 요청 본문 크기 상한(D16, {@code app.request.max-body-size}, 기본 1MB)을 모든 요청에 건다. */
@Configuration
public class WebLimitsConfig {

	@Bean
	public FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilter(
		@Value("${app.request.max-body-size:1MB}") DataSize maxBodySize, ObjectMapper objectMapper) {
		FilterRegistrationBean<RequestBodyLimitFilter> registration =
			new FilterRegistrationBean<>(new RequestBodyLimitFilter(maxBodySize, objectMapper));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
