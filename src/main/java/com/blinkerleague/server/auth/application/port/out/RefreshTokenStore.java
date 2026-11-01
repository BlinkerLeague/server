package com.blinkerleague.server.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh 토큰 저장소. 토큰 원문이 아닌 해시만 다룬다.
 * 운영은 Redis 구현을 쓴다(키 설계는 plan.md 8.3).
 */
public interface RefreshTokenStore {

	/** 토큰 해시 → driverId를 저장하고, 운전자의 현재 토큰 해시로 기록한다. */
	void save(String tokenHash, Long driverId, Duration ttl);

	/** 토큰 해시를 꺼내면서 지운다(Redis {@code GETDEL}). 같은 토큰은 한 번만 꺼낼 수 있다. */
	Optional<Long> consume(String tokenHash);

	/** 재발급에 사용한 토큰 해시를 기록한다. 같은 토큰이 다시 오면 탈취로 판단하는 데 쓴다. */
	void markUsed(String tokenHash, Long driverId, Duration ttl);

	/** 사용한 토큰 해시의 주인 driverId. */
	Optional<Long> findUsedOwner(String tokenHash);

	/** 운전자의 현재 토큰을 지운다. */
	void revokeAll(Long driverId);
}
