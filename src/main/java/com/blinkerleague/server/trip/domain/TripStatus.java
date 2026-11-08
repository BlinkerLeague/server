package com.blinkerleague.server.trip.domain;

/**
 * 운행 상태. 업로드 시 EXCLUDED_SHORT 또는 UPLOADED, 점수 계산 후 SCORED 또는 NO_TURN(plan.md 3.4, D10).
 * NO_TURN은 이름은 그대로 두고 "측정할 수 있는 항목이 하나도 없음"을 뜻한다(D28). 회전이 없어도 다른 항목을 측정하면 SCORED다.
 */
public enum TripStatus {
	UPLOADED,
	SCORED,
	NO_TURN,
	EXCLUDED_SHORT
}
