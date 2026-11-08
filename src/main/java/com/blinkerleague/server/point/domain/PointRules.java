package com.blinkerleague.server.point.domain;

/** 포인트 적립 규칙(plan.md 4.2). 값은 기준값 설정(point)에서 받는다. */
public final class PointRules {

	private PointRules() {
	}

	/** floor(min(거리, 최대 거리) × 점수 ÷ 100). */
	public static int tripPoints(double distanceKm, int score, int tripMaxKm) {
		return (int) Math.floor(Math.min(distanceKm, tripMaxKm) * score / 100);
	}

	/** 오늘 적립 합계가 일일 상한에 닿으면 남은 만큼만 준다. 이미 넘었으면 0. */
	public static int capToDailyLimit(int requested, int todayEarned, int dailyCap) {
		return Math.max(0, Math.min(requested, dailyCap - todayEarned));
	}
}
