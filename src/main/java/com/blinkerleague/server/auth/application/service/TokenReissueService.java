package com.blinkerleague.server.auth.application.service;

import com.blinkerleague.server.auth.application.port.out.AccessTokenIssuer;

import lombok.RequiredArgsConstructor;

/** Refresh 토큰으로 Access · Refresh 토큰을 함께 재발급한다. */
@RequiredArgsConstructor
public class TokenReissueService {

	private final RefreshTokenService refreshTokenService;
	private final AccessTokenIssuer accessTokenIssuer;

	public TokenPair reissue(String refreshToken) {
		RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
		return new TokenPair(accessTokenIssuer.issueAccessToken(rotation.driverId()), rotation.refreshToken());
	}

	public record TokenPair(String accessToken, String refreshToken) {
	}
}
