package com.blinkerleague.server.driver.adapter.out.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.auth.application.service.RefreshTokenService;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort.DriverTokens;
import com.blinkerleague.server.support.InMemoryRefreshTokenStore;

class AuthDriverTokensAdapterTest {

	private final RefreshTokenService refreshTokenService =
		new RefreshTokenService(new InMemoryRefreshTokenStore(), Duration.ofDays(30));
	private final AuthDriverTokensAdapter adapter =
		new AuthDriverTokensAdapter(driverId -> "access-for-" + driverId, refreshTokenService);

	@Test
	void 운전자의_액세스_토큰과_그_운전자로_재발급할_수_있는_리프레시_토큰을_발급한다() {
		DriverTokens tokens = adapter.issue(7L);

		assertThat(tokens.accessToken()).isEqualTo("access-for-7");
		assertThat(refreshTokenService.rotate(tokens.refreshToken()).driverId()).isEqualTo(7L);
	}
}
