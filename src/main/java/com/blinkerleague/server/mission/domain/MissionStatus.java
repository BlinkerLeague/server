package com.blinkerleague.server.mission.domain;

/** 운전자의 그날(period_key) 미션 상태(plan.md 3.6). */
public enum MissionStatus {
	IN_PROGRESS,
	ACHIEVED,
	FAILED;

	/** 그날이 끝났는지에 따라 최종 상태로 정한다. */
	public MissionStatus settle(boolean dayEnded) {
		throw new UnsupportedOperationException();
	}
}
