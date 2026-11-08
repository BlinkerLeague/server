package com.blinkerleague.server.trip.application.port.out;

/** 점수가 난 운행의 포인트 적립(plan.md 4.2). 같은 운행은 한 번만 적립되므로 다시 불러도 안전하다. */
public interface TripPointPort {

	void earnForTrip(Long driverId, String tripId, double distanceKm, int score);
}
