package com.blinkerleague.server.trip.domain;

/** 운행 상태. 업로드 시 EXCLUDED_SHORT 또는 UPLOADED, 점수 계산 후 SCORED 또는 NO_TURN(plan.md 3.4, D10). */
public enum TripStatus {
	UPLOADED,
	SCORED,
	NO_TURN,
	EXCLUDED_SHORT
}
