package com.blinkerleague.server.mission.domain;

/** 운전자의 그날(period_key) 미션 상태(plan.md 3.6). */
public enum MissionStatus {
	IN_PROGRESS,
	ACHIEVED,
	FAILED;

	/** 그날이 끝날 때까지 달성하지 못했으면 FAILED다(배치 없이 판정할 때 정한다). */
	public MissionStatus settle(boolean dayEnded) {
		return dayEnded && this == IN_PROGRESS ? FAILED : this;
	}
}
