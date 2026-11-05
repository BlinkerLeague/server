package com.blinkerleague.server.trip.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운행의 항목별 점수(plan.md 3.5 trip_metric). 측정값(0~1), 점수(0~100), 재분배 후 가중치. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMetric {

	private ScoreItem metric;

	private double value;

	private int score;

	private double weight;

	public TripMetric(ScoreItem metric, double value, int score, double weight) {
		this.metric = metric;
		this.value = value;
		this.score = score;
		this.weight = weight;
	}
}
