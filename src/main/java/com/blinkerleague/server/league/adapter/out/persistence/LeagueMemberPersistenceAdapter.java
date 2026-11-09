package com.blinkerleague.server.league.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.LeagueMemberStore;
import com.blinkerleague.server.league.domain.LeagueMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeagueMemberPersistenceAdapter implements LeagueMemberStore {

	private final LeagueMemberJpaRepository repository;

	@Override
	public Optional<LeagueMember> find(String seasonId, Long driverId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveIfAbsent(LeagueMember member) {
		throw new UnsupportedOperationException();
	}
}
