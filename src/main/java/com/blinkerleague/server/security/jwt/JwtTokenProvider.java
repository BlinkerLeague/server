package com.blinkerleague.server.security.jwt;

import java.time.Clock;

import org.springframework.security.oauth2.jwt.JwtEncoder;

import lombok.RequiredArgsConstructor;

/** 운전자 Access 토큰(JWT)을 발급한다. {@code sub}는 driverId, {@code iat}/{@code exp}는 {@link Clock} 기준이다. */
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public String issueAccessToken(Long driverId) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}
}
