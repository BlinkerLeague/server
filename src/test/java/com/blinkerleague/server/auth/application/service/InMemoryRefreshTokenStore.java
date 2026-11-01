package com.blinkerleague.server.auth.application.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.blinkerleague.server.auth.application.port.out.RefreshTokenStore;

/** 단위 테스트용 저장소. TTL은 다루지 않는다. */
class InMemoryRefreshTokenStore implements RefreshTokenStore {

	private final Map<String, Long> tokens = new HashMap<>();
	private final Map<Long, String> currentByDriver = new HashMap<>();
	private final Map<String, Long> used = new HashMap<>();

	@Override
	public void save(String tokenHash, Long driverId, Duration ttl) {
		tokens.put(tokenHash, driverId);
		currentByDriver.put(driverId, tokenHash);
	}

	@Override
	public Optional<Long> consume(String tokenHash) {
		return Optional.ofNullable(tokens.remove(tokenHash));
	}

	@Override
	public void markUsed(String tokenHash, Long driverId, Duration ttl) {
		used.put(tokenHash, driverId);
	}

	@Override
	public Optional<Long> findUsedOwner(String tokenHash) {
		return Optional.ofNullable(used.get(tokenHash));
	}

	@Override
	public void revokeAll(Long driverId) {
		String current = currentByDriver.remove(driverId);
		if (current != null) {
			tokens.remove(current);
		}
	}

	Set<String> storedHashes() {
		return tokens.keySet();
	}
}
