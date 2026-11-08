package com.blinkerleague.server.point.domain;

/** 포인트 적립 규칙(plan.md 4.2). 값은 기준값 설정(point)에서 받는다. */
public final class PointRules {

	private PointRules() {
	}

	public static int tripPoints(double distanceKm, int score, int tripMaxKm) {
		throw new UnsupportedOperationException();
	}
}
