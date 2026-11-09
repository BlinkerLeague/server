package com.blinkerleague.server.league.application.port.in;

import java.time.Instant;

/** 참여 운행을 올린 운전자를 그 운행이 끝난 시즌의 리그에 소속시킨다(D12). 다시 불러도 소속은 바뀌지 않는다. */
public interface JoinSeasonUseCase {

	void joinSeasonOf(Long driverId, Instant tripEndedAt);
}
