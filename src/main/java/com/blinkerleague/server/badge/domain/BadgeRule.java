package com.blinkerleague.server.badge.domain;

/** 배지 획득 조건의 종류. 목표값(target)은 배지 카탈로그에 둔다. */
public enum BadgeRule {
	/** 점수가 난 운행 수 */
	SCORED_TRIPS,
	/** 미션 최장 연속 달성 일수 */
	STREAK_DAYS,
	/** 깜빡이를 켠 회전 누적 수 */
	SIGNAL_ON_TURNS
}
