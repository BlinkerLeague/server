package com.blinkerleague.server.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import com.blinkerleague.server.auth.application.port.out.RefreshTokenStore;
import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Refresh 토큰 발급 · 재발급(rotation) · 강제 로그아웃.
 * 저장소에는 토큰의 SHA-256 해시(소문자 hex)만 저장한다.
 */
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenStore store;
	private final Duration refreshTtl;

	/** 새 Refresh 토큰을 만들어 저장하고 원문을 돌려준다. */
	public String issue(Long driverId) {
		String token = newToken();
		store.save(hash(token), driverId, refreshTtl);
		return token;
	}

	/**
	 * Refresh 토큰을 새 토큰으로 교체한다. 저장되지 않았거나 이미 사용한 토큰이면
	 * {@code INVALID_TOKEN}을 던지고, 이미 사용한 토큰이면 그 운전자의 현재 토큰도 지운다.
	 */
	public Rotation rotate(String refreshToken) {
		String tokenHash = hash(refreshToken);
		Optional<Long> owner = store.consume(tokenHash);
		if (owner.isEmpty()) {
			// 이미 사용한 토큰이 다시 왔다면 탈취로 보고 그 운전자의 현재 토큰까지 끊는다.
			store.findUsedOwner(tokenHash).ifPresent(store::revokeAll);
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		Long driverId = owner.get();
		store.markUsed(tokenHash, driverId, refreshTtl);
		return new Rotation(driverId, issue(driverId));
	}

	/** 운전자의 Refresh 토큰을 무효화한다(강제 로그아웃). */
	public void revoke(Long driverId) {
		store.revokeAll(driverId);
	}

	private static String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
		}
	}

	public record Rotation(Long driverId, String refreshToken) {
	}
}
