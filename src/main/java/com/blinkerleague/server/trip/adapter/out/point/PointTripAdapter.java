package com.blinkerleague.server.trip.adapter.out.point;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.point.application.port.in.EarnPointUseCase;
import com.blinkerleague.server.trip.application.port.out.TripPointPort;

import lombok.RequiredArgsConstructor;

/** trip의 적립 포트를 point 유스케이스로 연결한다. */
@Component
@RequiredArgsConstructor
public class PointTripAdapter implements TripPointPort {

	private final EarnPointUseCase earnPointUseCase;

	@Override
	public void earnForTrip(Long driverId, String tripId, double distanceKm, int score) {
		earnPointUseCase.earnForTrip(driverId, tripId, distanceKm, score);
	}
}
