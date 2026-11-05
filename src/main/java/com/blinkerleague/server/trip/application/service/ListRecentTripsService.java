package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.application.port.out.LoadRecentTripsPort;
import com.blinkerleague.server.trip.domain.TripStatus;

import lombok.RequiredArgsConstructor;

/** 최근 경기 목록(plan.md 3.5 GET /api/trips, SCR-01). */
@RequiredArgsConstructor
public class ListRecentTripsService {

	private static final int DEFAULT_LIMIT = 10;

	private final LoadRecentTripsPort loadRecentTripsPort;
	private final ThresholdProvider thresholdProvider;

	public List<RecentTrip> list(Long driverId, Integer limit) {
		Thresholds thresholds = thresholdProvider.current().thresholds();
		return loadRecentTripsPort.findRecentByDriver(driverId, DEFAULT_LIMIT).stream()
			.map(trip -> new RecentTrip(trip.getId(), trip.getEndedAt(), trip.getDistanceKm(), trip.getStatus(),
				trip.getScore(), TripScoreView.gradeOf(trip.getScore(), thresholds)))
			.toList();
	}

	public record RecentTrip(String tripId, Instant endedAt, double distanceKm, TripStatus status, Integer score,
		String grade) {
	}
}
