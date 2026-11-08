package com.blinkerleague.server.mission.domain;

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

/** 운전자의 그날(period_key) 미션 기록(plan.md 3.6 driver_mission). */
@Getter
@Entity
@Table(name = "driver_mission", uniqueConstraints = @UniqueConstraint(name = "uk_driver_mission_period",
	columnNames = {"driver_id", "mission_id", "period_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverMission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "driver_mission_id")
	private Long id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "mission_id", nullable = false)
	private Long missionId;

	@Column(name = "period_key", nullable = false, length = 10)
	private String periodKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MissionStatus status;

	@Column(name = "achieved_at")
	private Instant achievedAt;

	private DriverMission(Long driverId, Long missionId, String periodKey) {
		this.driverId = driverId;
		this.missionId = missionId;
		this.periodKey = periodKey;
		this.status = MissionStatus.IN_PROGRESS;
	}

	public static DriverMission start(Long driverId, Long missionId, String periodKey) {
		return new DriverMission(driverId, missionId, periodKey);
	}

	/** 그날 판정 결과를 반영한다. */
	public void apply(MissionStatus judged, Instant now) {
		this.status = judged;
		if (judged == MissionStatus.ACHIEVED) {
			this.achievedAt = now;
		}
	}
}
