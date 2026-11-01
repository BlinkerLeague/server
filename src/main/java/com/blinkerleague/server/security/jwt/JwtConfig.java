package com.blinkerleague.server.security.jwt;

import java.time.Clock;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/** HS256 JWT 인코더 · 디코더. 디코더의 만료 검사는 주입한 {@link Clock}을 기준으로 한다. */
public class JwtConfig {

	public JwtEncoder jwtEncoder(JwtProperties properties) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}
}
