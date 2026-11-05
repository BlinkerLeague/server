package com.blinkerleague.server.trip.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운행의 항목별 점수(plan.md 3.5 trip_metric). 측정값(0~1), 점수(0~100), 재분배 후 가중치. */
@Getter
@Entity
@Table(name = "trip_metric", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "metric"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripMetric {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Getter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	@Getter(AccessLevel.NONE)
	private Trip trip;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ScoreItem metric;

	// value는 H2 예약어라 컬럼 이름을 바꾼다.
	@Column(name = "metric_value", nullable = false)
	private double value;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private double weight;

	public TripMetric(ScoreItem metric, double value, int score, double weight) {
		this.metric = metric;
		this.value = value;
		this.score = score;
		this.weight = weight;
	}

	void assignTo(Trip trip) {
		this.trip = trip;
	}
}
