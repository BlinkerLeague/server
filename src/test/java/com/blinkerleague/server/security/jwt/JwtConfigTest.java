package com.blinkerleague.server.security.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

	@Test
	void secret이_설정되지_않으면_JWT_SECRET을_안내하며_거부한다() {
		JwtProperties missing = new JwtProperties(null, Duration.ofHours(1), Duration.ofDays(30));

		assertThatThrownBy(() -> new JwtConfig().jwtEncoder(missing))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void 환경변수가_치환되지_않은_값이면_미설정으로_보고_거부한다() {
		JwtProperties unresolved = new JwtProperties("${JWT_SECRET_NOT_PROVIDED_PLACEHOLDER}", Duration.ofHours(1),
			Duration.ofDays(30));

		assertThatThrownBy(() -> new JwtConfig().jwtEncoder(unresolved))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("secret이 없습니다");
	}
}
