package com.blinkerleague.server.trip.application.port.out;

/** 운행 업로드 후 배지 판정(plan.md 4.3). 이미 받은 배지는 다시 주지 않으므로 다시 불러도 안전하다. */
public interface TripBadgePort {

	void awardEarned(Long driverId);
}
