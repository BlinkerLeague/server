package com.blinkerleague.server.score.domain;

/** 항목 측정 상태(plan.md 4.1, SCR-02 회색 항목). */
public enum MetricStatus {
	/** 점수를 매김 */
	SCORED,
	/** 차량이 이 항목에 필요한 신호를 지원하지 않음 */
	UNSUPPORTED,
	/** 측정할 상황이 없거나 표본이 모자람 */
	NOT_APPLICABLE
}
