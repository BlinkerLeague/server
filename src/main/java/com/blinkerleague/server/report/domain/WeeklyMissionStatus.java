package com.blinkerleague.server.report.domain;

/** 주간 맞춤 미션 상태. 리포트를 만들 때 자동 등록되고(D7 B), 그 주가 끝난 뒤 다음 리포트에서 판정한다. */
public enum WeeklyMissionStatus {
	ASSIGNED,
	ACHIEVED,
	FAILED
}
