package com.blinkerleague.server.threshold.domain;

/** 운행 점수를 이루는 항목. 가중치 · 팁 문구의 키로 쓴다. */
public enum ScoreItem {
	TURN_SIGNAL,
	SMOOTH_DRIVING,
	SAFETY_WARNING,
	NIGHT_HEADLIGHT,
	/** 깜빡이를 켠 채 회전 없이 달림(D18) */
	SIGNAL_CANCEL,
	/** 회전 중 최고 속도(D18) */
	TURN_SPEED
}
