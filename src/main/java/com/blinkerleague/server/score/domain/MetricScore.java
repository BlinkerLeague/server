package com.blinkerleague.server.score.domain;

import java.util.Map;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/**
 * 항목 하나의 결과. 측정했으면 측정값(0~1) · 점수(0~100) · 재분배 후 가중치를 담고,
 * 제외됐으면 status · reason만 담는다(value · score · weight는 null). detail은 화면에 보여 줄 근거 수치다.
 */
public record MetricScore(ScoreItem metric, MetricStatus status, String reason, Double value, Integer score,
	Double weight, Map<String, Number> detail) {

	public static MetricScore scored(ScoreItem metric, double value, int score, double weight,
		Map<String, Number> detail) {
		return new MetricScore(metric, MetricStatus.SCORED, null, value, score, weight, detail);
	}

	public static MetricScore excluded(ScoreItem metric, MetricStatus status, String reason) {
		return new MetricScore(metric, status, reason, null, null, null, Map.of());
	}
}
