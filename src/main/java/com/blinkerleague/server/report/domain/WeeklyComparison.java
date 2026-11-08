package com.blinkerleague.server.report.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 이번 주와 지난주의 항목별 비교(plan.md 4.5). */
public record WeeklyComparison(List<MetricChange> metrics) {

	public static WeeklyComparison of(Map<ScoreItem, Integer> thisWeek, Map<ScoreItem, Integer> lastWeek) {
		throw new UnsupportedOperationException();
	}

	/** 이번 주 평균 점수가 가장 낮은 항목. 같으면 항목 순서가 앞선 것. 이번 주 측정한 항목이 없으면 빈 값. */
	public Optional<ScoreItem> weakest() {
		throw new UnsupportedOperationException();
	}
}
