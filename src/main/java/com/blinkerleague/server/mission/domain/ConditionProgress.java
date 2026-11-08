package com.blinkerleague.server.mission.domain;

/** 미션 조건 하나의 진행도(SCR-01 진행 링, SCR-03). */
public record ConditionProgress(ConditionType type, int current, int target) {

	public enum ConditionType {
		TURN_COUNT,
		SIGNAL_RATE
	}
}
