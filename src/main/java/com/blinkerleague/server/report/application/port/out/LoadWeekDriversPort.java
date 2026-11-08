package com.blinkerleague.server.report.application.port.out;

import java.time.Instant;
import java.util.List;

public interface LoadWeekDriversPort {

	/** 종료 시각이 [from, to)인 운행이 있는 운전자들. */
	List<Long> driversWithTrips(Instant from, Instant to);
}
