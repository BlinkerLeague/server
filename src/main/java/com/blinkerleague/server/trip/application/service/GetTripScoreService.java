package com.blinkerleague.server.trip.application.service;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;

import lombok.RequiredArgsConstructor;

/** 운행 점수 조회(plan.md 3.5 GET /api/trips/{tripId}/score). */
@RequiredArgsConstructor
public class GetTripScoreService {

	private final LoadTripPort loadTripPort;
	private final ThresholdProvider thresholdProvider;

	public TripScoreView get(Long driverId, String tripId) {
		return TripScoreView.of(loadTripPort.findById(tripId).orElseThrow(),
			thresholdProvider.current().thresholds());
	}
}
