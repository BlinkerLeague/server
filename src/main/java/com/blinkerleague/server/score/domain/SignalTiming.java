package com.blinkerleague.server.score.domain;

/** 회전 깜빡이 4단계(plan.md 4.1, D18). 앱이 회전 지점 30m 전을 기준으로 판정한다. */
public enum SignalTiming {
	ON_TIME,
	LATE,
	WRONG_DIRECTION,
	NONE
}
