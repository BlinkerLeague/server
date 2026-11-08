package com.blinkerleague.server.report.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 이번 주와 지난주의 항목별 비교(plan.md 4.5). */
public record WeeklyComparison(List<MetricChange> metrics) {

	public static WeeklyComparison of(Map<ScoreItem, Integer> thisWeek, Map<ScoreItem, Integer> lastWeek) {
		List<MetricChange> metrics = Arrays.stream(ScoreItem.values())
			.filter(item -> thisWeek.containsKey(item) || lastWeek.containsKey(item))
			.map(item -> {
				Integer now = thisWeek.get(item);
				Integer before = lastWeek.get(item);
				return new MetricChange(item, now, before, now == null || before == null ? null : now - before);
			})
			.toList();
		return new WeeklyComparison(metrics);
	}

	/** 이번 주 평균 점수가 가장 낮은 항목. 같으면 항목 순서가 앞선 것. 이번 주 측정한 항목이 없으면 빈 값. */
	public Optional<ScoreItem> weakest() {
		// metrics는 항목 순서라 min은 동점일 때 앞선 항목을 고른다.
		return metrics.stream()
			.filter(m -> m.thisWeek() != null)
			.min(Comparator.comparingInt(MetricChange::thisWeek))
			.map(MetricChange::metric);
	}
}
