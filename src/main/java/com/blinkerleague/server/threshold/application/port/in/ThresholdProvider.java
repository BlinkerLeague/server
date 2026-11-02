package com.blinkerleague.server.threshold.application.port.in;

import com.blinkerleague.server.threshold.domain.VersionedThresholds;

/** 현재 기준값. 점수 · 미션 · 포인트 계산도 상수 대신 여기서 읽는다(plan.md 6장). */
public interface ThresholdProvider {

	VersionedThresholds current();
}
