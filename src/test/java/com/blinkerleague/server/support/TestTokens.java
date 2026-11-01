package com.blinkerleague.server.support;

import java.time.Clock;
import java.time.Duration;

import com.blinkerleague.server.auth.adapter.out.jwt.JwtAccessTokenIssuer;
import com.blinkerleague.server.security.jwt.JwtConfig;
import com.blinkerleague.server.security.jwt.JwtProperties;

/** 통합 테스트용 Access 토큰. {@link #JWT}는 application-test.yaml의 jwt 설정과 같다. */
public final class TestTokens {

	public static final JwtProperties JWT = new JwtProperties(
		"blinkerleague-test-secret-key-32bytes-or-more!", Duration.ofHours(1), Duration.ofDays(30));

	private TestTokens() {
	}

	public static String accessToken(Long driverId) {
		return accessToken(JWT, driverId, Clock.systemUTC());
	}

	public static String accessToken(JwtProperties properties, Long driverId, Clock clock) {
		return new JwtAccessTokenIssuer(new JwtConfig().jwtEncoder(properties), properties, clock)
			.issueAccessToken(driverId);
	}
}
