package com.blinkerleague.server.trip.domain;

/**
 * 운행 중 이벤트 종류. P0은 TURN만 점수에 쓰고 나머지는 저장만 한다.
 * LANE_CHANGE는 차량 앱의 감지 가능성 확인 후 P1에서 깜빡이 항목에 반영한다(plan.md D14).
 */
public enum TripEventType {
	TURN,
	LANE_CHANGE,
	HARSH_ACCEL,
	HARSH_BRAKE,
	SAFETY_WARNING,
	NIGHT_SEGMENT
}
