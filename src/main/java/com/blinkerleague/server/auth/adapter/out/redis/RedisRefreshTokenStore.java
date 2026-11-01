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
	private static final String DRIVER_KEY = "auth:refresh:driver:";
	private static final String USED_KEY = "auth:refresh:used:";

	private final StringRedisTemplate redis;

	@Override
	public void save(String tokenHash, Long driverId, Duration ttl) {
		redis.opsForValue().set(TOKEN_KEY + tokenHash, String.valueOf(driverId), ttl);
		redis.opsForValue().set(DRIVER_KEY + driverId, tokenHash, ttl);
	}

	@Override
	public Optional<Long> consume(String tokenHash) {
		// GETDEL: 꺼내면서 지우므로 같은 토큰으로 동시에 요청해도 한 번만 성공한다.
		return Optional.ofNullable(redis.opsForValue().getAndDelete(TOKEN_KEY + tokenHash)).map(Long::valueOf);
	}

	@Override
	public void markUsed(String tokenHash, Long driverId, Duration ttl) {
		redis.opsForValue().set(USED_KEY + tokenHash, String.valueOf(driverId), ttl);
	}

	@Override
	public Optional<Long> findUsedOwner(String tokenHash) {
		return Optional.ofNullable(redis.opsForValue().get(USED_KEY + tokenHash)).map(Long::valueOf);
	}

	@Override
	public void revokeAll(Long driverId) {
		String currentHash = redis.opsForValue().getAndDelete(DRIVER_KEY + driverId);
		if (currentHash != null) {
			redis.delete(TOKEN_KEY + currentHash);
		}
	}
}
