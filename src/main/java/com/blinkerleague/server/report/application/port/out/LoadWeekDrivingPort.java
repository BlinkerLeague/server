package com.blinkerleague.server.report.application.port.out;

import java.time.Instant;

import com.blinkerleague.server.report.domain.WeekDriving;

public interface LoadWeekDrivingPort {

	/** 종료 시각이 [from, to)인 운전자의 운행 기록. */
	WeekDriving load(Long driverId, Instant from, Instant to);
}
