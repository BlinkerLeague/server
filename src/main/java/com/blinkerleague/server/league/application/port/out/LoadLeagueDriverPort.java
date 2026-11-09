package com.blinkerleague.server.league.application.port.out;

import java.util.Optional;

/** 리그가 읽는 운전자 정보. */
public interface LoadLeagueDriverPort {

	Optional<LeagueDriver> find(Long driverId);

	/** 지금 등록한 시군구 코드와 리그 순위 공개 여부(D8). */
	record LeagueDriver(String regionCode, boolean visible) {
	}
}
