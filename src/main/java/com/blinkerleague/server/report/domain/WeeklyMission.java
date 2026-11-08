package com.blinkerleague.server.report.domain;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;
import com.blinkerleague.server.threshold.domain.ScoreItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운전자의 주간 맞춤 미션(plan.md 4.5). 지난주 가장 약한 항목으로 만들어 다음 주에 자동 등록한다(D7 B).
 * 그 주 해당 항목의 평균 점수가 target 이상이고 주간 거리가 minDistanceKm 이상이면 달성이다.
 */
@Getter
@Entity
@Table(name = "weekly_mission", uniqueConstraints = @UniqueConstraint(name = "uk_weekly_mission_driver_week",
	columnNames = {"driver_id", "week_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyMission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "weekly_mission_id")
	private Long id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	/** 미션을 수행할 주(2026-W46). */
	@Column(name = "week_key", nullable = false, length = 8)
	private String weekKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ScoreItem metric;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(nullable = false)
	private int target;

	@Column(name = "min_distance_km", nullable = false)
	private double minDistanceKm;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WeeklyMissionStatus status;

	public WeeklyMission(Long driverId, String weekKey, ScoreItem metric, String title, int target,
		double minDistanceKm) {
		this.driverId = driverId;
		this.weekKey = weekKey;
		this.metric = metric;
		this.title = title;
		this.target = target;
		this.minDistanceKm = minDistanceKm;
		this.status = WeeklyMissionStatus.ASSIGNED;
	}

	/** 그 주의 해당 항목 평균 점수(측정하지 않았으면 null)와 주간 거리로 판정한다. */
	public void judge(Integer weekAverage, double weekDistanceKm) {
		throw new UnsupportedOperationException();
	}
}
