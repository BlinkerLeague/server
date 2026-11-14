package com.blinkerleague.server.mission.domain;

import java.time.LocalDate;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 미션 정의(plan.md 3.6). active_from ~ active_to(포함) 동안 운영한다. */
@Getter
@Entity
@Table(name = "mission")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mission_id")
	private Long id;

	@Column(nullable = false, length = 40)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MissionPeriod period;

	@Convert(converter = TurnSignalConditionConverter.class)
	@Column(name = "condition_json", nullable = false)
	private TurnSignalCondition condition;

	@Column(name = "reward_point", nullable = false)
	private int rewardPoint;

	@Column(name = "active_from", nullable = false)
	private LocalDate activeFrom;

	@Column(name = "active_to", nullable = false)
	private LocalDate activeTo;

	private Mission(String title, MissionPeriod period, TurnSignalCondition condition, int rewardPoint,
		LocalDate activeFrom, LocalDate activeTo) {
		this.title = title;
		this.period = period;
		this.condition = condition;
		this.rewardPoint = rewardPoint;
		this.activeFrom = activeFrom;
		this.activeTo = activeTo;
	}

	public static Mission daily(String title, TurnSignalCondition condition, int rewardPoint, LocalDate activeFrom,
		LocalDate activeTo) {
		return new Mission(title, MissionPeriod.DAILY, condition, rewardPoint, activeFrom, activeTo);
	}
}
