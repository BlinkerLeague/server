package com.blinkerleague.server.driver.adapter.out.auth;

import com.blinkerleague.server.auth.application.port.out.AccessTokenIssuer;
import com.blinkerleague.server.auth.application.service.RefreshTokenService;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort;

import lombok.RequiredArgsConstructor;

/** driver의 토큰 발급 포트를 auth 기능(Access JWT 발급, Refresh 토큰 저장)으로 구현한다. */
@RequiredArgsConstructor
public class AuthDriverTokensAdapter implements IssueDriverTokensPort {

	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshTokenService refreshTokenService;

	@Override
	public DriverTokens issue(Long driverId) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}
}
