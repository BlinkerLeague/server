package com.blinkerleague.server.league.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.league.domain.LeagueMember;

/** 시즌 리그 소속(D12). 운전자 · 시즌마다 하나다. */
public interface LeagueMemberStore {

	Optional<LeagueMember> find(String seasonId, Long driverId);

	/** 저장한다. 같은 운전자 · 시즌이 동시에 먼저 저장됐으면 아무 일도 하지 않는다. */
	void saveIfAbsent(LeagueMember member);
}
