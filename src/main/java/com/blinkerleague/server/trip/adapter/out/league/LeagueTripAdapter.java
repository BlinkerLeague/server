package com.blinkerleague.server.trip.adapter.out.league;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.in.JoinSeasonUseCase;
import com.blinkerleague.server.trip.application.port.out.TripLeaguePort;

import lombok.RequiredArgsConstructor;

/** trip의 리그 포트를 league 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class LeagueTripAdapter implements TripLeaguePort {

	private final JoinSeasonUseCase joinSeasonUseCase;

	@Override
	public void joinSeasonOf(Long driverId, Instant tripEndedAt) {
		joinSeasonUseCase.joinSeasonOf(driverId, tripEndedAt);
	}
}
