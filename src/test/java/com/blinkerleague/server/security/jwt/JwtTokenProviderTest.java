package com.blinkerleague.server.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.blinkerleague.server.common.time.TimeConfig;

class JwtTokenProviderTest {

	private static final String SECRET = "blinkerleague-test-secret-key-32bytes-or-more!";
	private static final Instant ISSUED_AT = Instant.parse("2026-11-01T01:00:00Z");

	private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1), Duration.ofDays(30));
	private final JwtConfig jwtConfig = new JwtConfig();

	@Test
	void 발급한_액세스_토큰의_subject는_driverId다() {
		String token = providerAt(ISSUED_AT, properties).issueAccessToken(1L);

		Jwt jwt = decoderAt(ISSUED_AT).decode(token);

		assertThat(jwt.getSubject()).isEqualTo("1");
	}

	@Test
	void 액세스_토큰의_만료_시각은_발급_시각에_TTL을_더한_값이다() {
		String token = providerAt(ISSUED_AT, properties).issueAccessToken(1L);

		Jwt jwt = decoderAt(ISSUED_AT).decode(token);

		assertThat(jwt.getIssuedAt()).isEqualTo(ISSUED_AT);
		assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofHours(1)));
	}

	@Test
	void 만료_시각이_지난_토큰은_거부한다() {
		String token = providerAt(ISSUED_AT, properties).issueAccessToken(1L);
		JwtDecoder decoder = decoderAt(ISSUED_AT.plus(Duration.ofHours(1)).plusSeconds(1));

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void 다른_secret으로_서명한_토큰은_거부한다() {
		JwtProperties other = new JwtProperties("another-secret-key-that-is-also-32bytes-long!!", Duration.ofHours(1),
			Duration.ofDays(30));
		String forged = providerAt(ISSUED_AT, other).issueAccessToken(1L);
		JwtDecoder decoder = decoderAt(ISSUED_AT);

		assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(BadJwtException.class);
	}

	@Test
	void secret이_32바이트보다_짧으면_인코더를_만들_수_없다() {
		JwtProperties weak = new JwtProperties("too-short-secret", Duration.ofHours(1), Duration.ofDays(30));

		assertThatThrownBy(() -> jwtConfig.jwtEncoder(weak)).isInstanceOf(IllegalArgumentException.class);
	}

	private JwtTokenProvider providerAt(Instant now, JwtProperties props) {
		return new JwtTokenProvider(jwtConfig.jwtEncoder(props), props, Clock.fixed(now, TimeConfig.ZONE));
	}

	private JwtDecoder decoderAt(Instant now) {
		return jwtConfig.jwtDecoder(properties, Clock.fixed(now, TimeConfig.ZONE));
	}
}
