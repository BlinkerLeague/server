package com.blinkerleague.server.score.domain;

/** 회전 한 번의 경기 결과. 깜빡이를 켜면 득점, 켜지 않으면 실점(plan.md D22). */
public enum PlayResult {
	GOAL,
	CONCEDED
}
