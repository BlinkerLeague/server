package com.blinkerleague.server.common.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API 명세(OpenAPI) 공통 설정. SecurityConfig와 같이 기본은 Bearer JWT 인증이 필요하고,
 * 공개 API는 컨트롤러에서 {@code @SecurityRequirements()}로 인증 없음을 표시한다.
 */
@Configuration
public class OpenApiConfig {

	static final String BEARER_AUTH = "bearerAuth";

	private static final String DESCRIPTION = """
		깜빡이리그(BlinkerLeague) 차량 앱의 백엔드 API.

		- 인증: 보호된 API는 `Authorization: Bearer <accessToken>` 헤더가 필요하다. \
		  토큰은 `POST /api/drivers`(등록)에서 처음 받고, Access 토큰(1시간)이 만료되면 `POST /api/auth/refresh`로 다시 받는다.
		- 토큰이 없으면 401 `UNAUTHORIZED`, 위조 · 만료된 토큰은 401 `INVALID_TOKEN`.
		- 오류 응답은 모두 `{ "code", "message" }` 형식이다. 앱은 `code`로 분기하고 `message`는 사용자 안내용이다.
		- 시각은 ISO-8601(오프셋 포함, 예: `2026-11-03T08:00:00+09:00`), 일 단위 집계는 Asia/Seoul 기준.
		""";

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("깜빡이리그 서버 API").version("P0").description(DESCRIPTION))
			.components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
