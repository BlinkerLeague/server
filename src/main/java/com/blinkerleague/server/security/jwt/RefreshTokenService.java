package com.blinkerleague.server.security.jwt;

import lombok.RequiredArgsConstructor;

/**
 * Refresh 토큰 발급 · 재발급(rotation) · 강제 로그아웃.
 * 저장소에는 토큰의 SHA-256 해시(소문자 hex)만 저장한다.
 */
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenStore store;
	private final JwtProperties properties;

	/** 새 Refresh 토큰을 만들어 저장하고 원문을 돌려준다. */
	public String issue(Long driverId) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	/**
	 * Refresh 토큰을 새 토큰으로 교체한다. 저장되지 않았거나 이미 사용한 토큰이면
	 * {@code INVALID_TOKEN}을 던지고, 이미 사용한 토큰이면 그 운전자의 현재 토큰도 지운다.
	 */
	public Rotation rotate(String refreshToken) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	/** 운전자의 Refresh 토큰을 무효화한다(강제 로그아웃). */
	public void revoke(Long driverId) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	public record Rotation(Long driverId, String refreshToken) {
	}
}
