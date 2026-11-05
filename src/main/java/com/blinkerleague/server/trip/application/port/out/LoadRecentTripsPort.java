package com.blinkerleague.server.trip.application.port.out;

import java.util.List;

import com.blinkerleague.server.trip.domain.Trip;

public interface LoadRecentTripsPort {

	/** 운전자의 운행을 종료 시각 최신순으로. */
	List<Trip> findRecentByDriver(Long driverId);
}
