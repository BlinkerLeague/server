package com.blinkerleague.server.trip.domain;

/** 운행 중 이벤트 종류. P0은 TURN만 점수에 쓰고 나머지는 저장만 한다. */
public enum TripEventType {
	TURN,
	HARSH_ACCEL,
	HARSH_BRAKE,
	SAFETY_WARNING,
	NIGHT_SEGMENT
}
