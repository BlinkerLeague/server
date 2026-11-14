package com.blinkerleague.server.trip.application.port.out;

import java.time.Instant;

/** 참여 운행을 올린 운전자를 그 시즌 리그에 소속시킨다(plan.md 4.4, D12). 다시 불러도 소속은 바뀌지 않는다. */
public interface TripLeaguePort {

	void joinSeasonOf(Long driverId, Instant tripEndedAt);
}
