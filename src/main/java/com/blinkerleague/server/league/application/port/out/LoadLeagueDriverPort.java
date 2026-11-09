package com.blinkerleague.server.league.application.port.out;

import java.util.Optional;

/** 리그가 읽는 운전자 정보. */
public interface LoadLeagueDriverPort {

	/** 운전자가 지금 등록한 시군구 코드. */
	Optional<String> regionCodeOf(Long driverId);
}
