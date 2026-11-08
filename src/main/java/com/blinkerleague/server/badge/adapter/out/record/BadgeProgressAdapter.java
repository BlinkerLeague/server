package com.blinkerleague.server.badge.adapter.out.record;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.port.out.BadgeProgressPort;
import com.blinkerleague.server.badge.domain.BadgeProgress;

import lombok.RequiredArgsConstructor;

/** 저장된 운행(trip · trip_event)과 미션 기록(driver_mission)을 읽어 배지 판정용 누적 기록을 만든다. */
@Component
@RequiredArgsConstructor
public class BadgeProgressAdapter implements BadgeProgressPort {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public BadgeProgress progress(Long driverId) {
		throw new UnsupportedOperationException();
	}
}
