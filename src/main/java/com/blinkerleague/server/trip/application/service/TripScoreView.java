package com.blinkerleague.server.trip.application.service;

import java.util.List;

import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.trip.domain.Trip;
import com.blinkerleague.server.trip.domain.TripStatus;

/** 운행 결과(plan.md 3.5 TripResult). 업로드 응답과 점수 조회가 같은 구조를 쓴다. */
public record TripScoreView(String tripId, TripStatus status, Integer score, String grade, List<Metric> metrics) {

	static TripScoreView of(Trip trip, Thresholds thresholds) {
		String grade = trip.getScore() == null ? null : thresholds.gradeOf(trip.getScore()).orElse(null);
		List<Metric> metrics = trip.getMetrics().stream()
			.map(m -> new Metric(m.getMetric(), m.getValue(), m.getScore(), m.getWeight()))
			.toList();
		return new TripScoreView(trip.getId(), trip.getStatus(), trip.getScore(), grade, metrics);
	}

	public record Metric(ScoreItem metric, double value, int score, double weight) {
	}
}
