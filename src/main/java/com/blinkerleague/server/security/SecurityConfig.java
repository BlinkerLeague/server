package com.blinkerleague.server.security;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;

import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 차량 앱 전용 API 서버라 세션 · CSRF · 폼 로그인을 쓰지 않는다.
 * 운전자 인증은 Bearer Access 토큰(JWT)을 resource server로 검증한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final ObjectMapper objectMapper;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// H2 콘솔 서블릿이 함께 등록되면 문자열 매처가 MVC/비MVC를 구분하지 못하므로 Ant 매처를 명시한다.
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(antMatcher(HttpMethod.POST, "/api/drivers")).permitAll()
				.requestMatchers(antMatcher(HttpMethod.GET, "/api/config/**")).permitAll()
				.requestMatchers(antMatcher("/h2-console/**")).permitAll()
				.requestMatchers(antMatcher("/error")).permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth -> oauth
				.jwt(Customizer.withDefaults())
				.authenticationEntryPoint((request, response, e) -> writeError(response,
					e instanceof InvalidBearerTokenException ? ErrorCode.INVALID_TOKEN : ErrorCode.UNAUTHORIZED)))
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint((request, response, e) -> writeError(response, ErrorCode.UNAUTHORIZED))
				.accessDeniedHandler((request, response, e) -> writeError(response, ErrorCode.FORBIDDEN)));
		return http.build();
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
	}
}
