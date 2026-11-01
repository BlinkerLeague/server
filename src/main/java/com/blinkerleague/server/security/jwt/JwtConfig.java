package com.blinkerleague.server.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** HS256 JWT 인코더 · 디코더. 디코더의 만료 검사는 주입한 {@link Clock}을 기준으로 한다. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		// 기본 허용 오차(60초) 없이 만료 시각을 그대로 적용한다.
		JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
		timestampValidator.setClock(clock);
		decoder.setJwtValidator(timestampValidator);
		return decoder;
	}

	private static SecretKey secretKey(JwtProperties properties) {
		// 환경변수가 없으면 "${JWT_SECRET}" 같은 글자가 그대로 넘어오므로 미설정으로 본다.
		if (properties.secret() == null || properties.secret().isBlank() || properties.secret().startsWith("${")) {
			throw new IllegalArgumentException("JWT secret이 없습니다. 환경변수 JWT_SECRET을 설정하세요.");
		}
		byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
				"JWT secret은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다. 환경변수 JWT_SECRET을 확인하세요.");
		}
		return new SecretKeySpec(secret, "HmacSHA256");
	}
}
