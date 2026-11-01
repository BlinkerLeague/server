package com.blinkerleague.server.auth.adapter.out.jwt;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.blinkerleague.server.auth.application.port.out.AccessTokenIssuer;
import com.blinkerleague.server.security.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

/** 운전자 Access 토큰(JWT)을 발급한다. {@code sub}는 driverId, {@code iat}/{@code exp}는 {@link Clock} 기준이다. */
@RequiredArgsConstructor
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	@Override
	public String issueAccessToken(Long driverId) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(String.valueOf(driverId))
			.issuedAt(now)
			.expiresAt(now.plus(properties.accessTtl()))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
