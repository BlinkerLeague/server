package com.blinkerleague.server.report.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 한 주 항목별 평균 점수(0~100). 긴 운행이 더 크게 반영되도록 거리 가중 평균이다. */
public final class WeeklyAverages {

	private WeeklyAverages() {
	}

	public static Map<ScoreItem, Integer> of(List<TripMetricRecord> records) {
		Map<ScoreItem, double[]> sums = new EnumMap<>(ScoreItem.class);
		for (TripMetricRecord record : records) {
			double[] sum = sums.computeIfAbsent(record.metric(), m -> new double[2]);
			sum[0] += record.score() * record.distanceKm();
			sum[1] += record.distanceKm();
		}
		Map<ScoreItem, Integer> averages = new EnumMap<>(ScoreItem.class);
		sums.forEach((metric, sum) -> averages.put(metric, (int) Math.round(sum[0] / sum[1])));
		return averages;
	}
}
