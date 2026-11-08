package com.blinkerleague.server.report.domain;

import java.util.List;
import java.util.Map;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 한 주 항목별 평균 점수(0~100). 긴 운행이 더 크게 반영되도록 거리 가중 평균이다. */
public final class WeeklyAverages {

	private WeeklyAverages() {
	}

	public static Map<ScoreItem, Integer> of(List<TripMetricRecord> records) {
		throw new UnsupportedOperationException();
	}
}
