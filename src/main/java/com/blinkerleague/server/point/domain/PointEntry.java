package com.blinkerleague.server.point.domain;

import java.time.Instant;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

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
 * 포인트 원장 한 줄. 잔액은 따로 두지 않고 amount의 합으로 구한다.
 * 화면에 적립 당시의 거리 · 점수를 보여줘야 하므로 TRIP 적립은 그 값을 함께 남긴다.
 */
@Getter
@Entity
@Table(name = "point_ledger",
	uniqueConstraints = @UniqueConstraint(name = "uk_point_ledger_reason_ref", columnNames = {"reason", "ref_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointEntry extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ledger_id")
	private Long id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(nullable = false)
	private int amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointReason reason;

	@Column(name = "ref_id", nullable = false, length = 64)
	private String refId;

	@Column(name = "distance_km")
	private Double distanceKm;

	private Integer score;

	@Column(name = "earned_at", nullable = false)
	private Instant earnedAt;

	private PointEntry(Long driverId, int amount, PointReason reason, String refId, Double distanceKm,
		Integer score, Instant earnedAt) {
		this.driverId = driverId;
		this.amount = amount;
		this.reason = reason;
		this.refId = refId;
		this.distanceKm = distanceKm;
		this.score = score;
		this.earnedAt = earnedAt;
	}

	public static PointEntry trip(Long driverId, String tripId, int amount, double distanceKm, int score,
		Instant earnedAt) {
		return new PointEntry(driverId, amount, PointReason.TRIP, tripId, distanceKm, score, earnedAt);
	}

	public static PointEntry dailyMission(Long driverId, String refId, int amount, Instant earnedAt) {
		return new PointEntry(driverId, amount, PointReason.DAILY_MISSION, refId, null, null, earnedAt);
	}

	public static PointEntry streak(Long driverId, String refId, int amount, Instant earnedAt) {
		return new PointEntry(driverId, amount, PointReason.STREAK_7, refId, null, null, earnedAt);
	}
}
