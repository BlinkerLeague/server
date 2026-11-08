package com.blinkerleague.server.auth.adapter.in.web;

import java.util.List;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

	static {
		// @CurrentDriver는 요청 값이 아니라 Access 토큰의 sub에서 꺼내므로 API 문서의 파라미터에서 뺀다.
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentDriver.class);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentDriverArgumentResolver());
	}
}
