package com.blinkerleague.server.trip.application.service;

import java.time.Instant;
import java.util.List;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.application.port.out.LoadRecentTripsPort;
import com.blinkerleague.server.trip.domain.TripStatus;

import lombok.RequiredArgsConstructor;

/** 최근 경기 목록(plan.md 3.5 GET /api/trips, SCR-01). */
@RequiredArgsConstructor
public class ListRecentTripsService {

	private static final int DEFAULT_LIMIT = 10;
	private static final int MAX_LIMIT = 50;

	private final LoadRecentTripsPort loadRecentTripsPort;
	private final ThresholdProvider thresholdProvider;

	public List<RecentTrip> list(Long driverId, Integer limit) {
		Thresholds thresholds = thresholdProvider.current().thresholds();
		return loadRecentTripsPort.findRecentByDriver(driverId, effectiveLimit(limit)).stream()
			.map(trip -> new RecentTrip(trip.getId(), trip.getEndedAt(), trip.getDistanceKm(), trip.getStatus(),
				trip.getScore(), TripScoreView.gradeOf(trip.getScore(), thresholds)))
			.toList();
	}

	// 기본 10, 최대 50. 50을 넘으면 거부하지 않고 자른다(plan.md D24). 1 미만은 잘못된 요청이다.
	private static int effectiveLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		if (limit < 1) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "limit은 1 이상이어야 합니다.");
		}
		return Math.min(limit, MAX_LIMIT);
	}

	public record RecentTrip(String tripId, Instant endedAt, double distanceKm, TripStatus status, Integer score,
		String grade) {
	}
}
