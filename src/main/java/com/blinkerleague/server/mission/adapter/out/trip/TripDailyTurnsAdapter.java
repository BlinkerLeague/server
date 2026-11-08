package com.blinkerleague.server.mission.adapter.out.trip;

import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.LoadDailyTurnsPort;
import com.blinkerleague.server.mission.domain.DailyTurns;

import lombok.RequiredArgsConstructor;

/** 저장된 운행의 TURN 이벤트(payload.signalOn)로 그날 회전 결과를 센다. */
@Component
@RequiredArgsConstructor
public class TripDailyTurnsAdapter implements LoadDailyTurnsPort {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public DailyTurns load(Long driverId, Instant from, Instant to) {
		throw new UnsupportedOperationException();
	}
}
