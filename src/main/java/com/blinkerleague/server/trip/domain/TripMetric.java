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

/** 운행의 항목별 결과(plan.md 3.5 · 4.1 trip_metric). 측정한 항목은 측정값(0~1) · 점수(0~100) · 재분배 후 가중치, 측정하지 못한 항목은 상태 · 사유. */
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

	/** SCORED · UNSUPPORTED · NOT_APPLICABLE(plan.md 4.1). */
	@Column(nullable = false, length = 20)
	private String status;

	/** 측정하지 못한 이유(예: NO_TURN, SIGNAL_NOT_SUPPORTED). 측정했으면 null. */
	@Column(length = 30)
	private String reason;

	// value는 H2 예약어라 컬럼 이름을 바꾼다. 측정하지 못한 항목은 value · score · weight가 null이다.
	@Column(name = "metric_value")
	private Double value;

	private Integer score;

	private Double weight;

	/** 화면에 보여 줄 근거 수치(JSON 객체, 예: {"count":1,"per10Km":1.4}). */
	@Column(length = 500)
	private String detail;

	public TripMetric(ScoreItem metric, double value, int score, double weight) {
		this(metric, "SCORED", null, value, score, weight, "{}");
	}

	public TripMetric(ScoreItem metric, String status, String reason, Double value, Integer score, Double weight,
		String detail) {
		this.metric = metric;
		this.status = status;
		this.reason = reason;
		this.value = value;
		this.score = score;
		this.weight = weight;
		this.detail = detail;
	}

	void assignTo(Trip trip) {
		this.trip = trip;
	}
}
