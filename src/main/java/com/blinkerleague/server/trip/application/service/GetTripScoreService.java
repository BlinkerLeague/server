package com.blinkerleague.server.trip.application.service;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.trip.application.port.out.LoadTripPort;
import com.blinkerleague.server.trip.domain.Trip;

import lombok.RequiredArgsConstructor;

/** 운행 점수 조회(plan.md 3.5 GET /api/trips/{tripId}/score). */
@RequiredArgsConstructor
public class GetTripScoreService {

	private final LoadTripPort loadTripPort;
	private final ThresholdProvider thresholdProvider;

	public TripScoreView get(Long driverId, String tripId) {
		// 남의 운행은 있는지도 알리지 않도록 없는 운행과 같이 응답한다.
		Trip trip = loadTripPort.findById(tripId)
			.filter(found -> found.getDriverId().equals(driverId))
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "운행을 찾을 수 없습니다."));
		return TripScoreView.of(trip, thresholdProvider.current().thresholds());
	}
}
