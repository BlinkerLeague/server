package com.blinkerleague.server.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TokenReissueServiceTest {

	private final RefreshTokenService refreshTokenService =
		new RefreshTokenService(new InMemoryRefreshTokenStore(), Duration.ofDays(30));
	private final TokenReissueService service =
		new TokenReissueService(refreshTokenService, driverId -> "access-for-" + driverId);

	@Test
	void 리프레시_토큰으로_재발급하면_그_운전자의_새_액세스_토큰과_새_리프레시_토큰을_준다() {
		String refreshToken = refreshTokenService.issue(7L);

		TokenReissueService.TokenPair pair = service.reissue(refreshToken);

		assertThat(pair.accessToken()).isEqualTo("access-for-7");
		assertThat(pair.refreshToken()).isNotBlank().isNotEqualTo(refreshToken);
	}
}
