package com.blinkerleague.server.report.adapter.out.record;

import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.report.application.port.out.LoadWeekDrivingPort;
import com.blinkerleague.server.report.domain.WeekDriving;

import lombok.RequiredArgsConstructor;

/** 저장된 운행(trip)과 항목별 점수(trip_metric)를 읽어 한 주 기록을 만든다. */
@Component
@RequiredArgsConstructor
public class WeekDrivingAdapter implements LoadWeekDrivingPort {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public WeekDriving load(Long driverId, Instant from, Instant to) {
		throw new UnsupportedOperationException();
	}
}
