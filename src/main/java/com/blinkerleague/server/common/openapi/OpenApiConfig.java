package com.blinkerleague.server.common.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API 명세(OpenAPI) 공통 설정. SecurityConfig와 같이 기본은 Bearer JWT 인증이 필요하고,
 * 공개 API는 컨트롤러에서 {@code @SecurityRequirements()}로 인증 없음을 표시한다.
 */
@Configuration
public class OpenApiConfig {

	static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
