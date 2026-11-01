package com.blinkerleague.server.auth.adapter.out.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.blinkerleague.server.auth.application.port.out.RefreshTokenStore;

import lombok.RequiredArgsConstructor;

/** Refresh 토큰 해시를 Redis에 저장한다. 키 설계는 plan.md 8.3. */
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String TOKEN_KEY = "auth:refresh:";

	private final StringRedisTemplate redis;

	@Override
	public void save(String tokenHash, Long driverId, Duration ttl) {
		redis.opsForValue().set(TOKEN_KEY + tokenHash, String.valueOf(driverId));
	}

	@Override
	public Optional<Long> consume(String tokenHash) {
		return Optional.ofNullable(redis.opsForValue().get(TOKEN_KEY + tokenHash)).map(Long::valueOf);
	}

	@Override
	public void markUsed(String tokenHash, Long driverId, Duration ttl) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	@Override
	public Optional<Long> findUsedOwner(String tokenHash) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	@Override
	public void revokeAll(Long driverId) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}
}
