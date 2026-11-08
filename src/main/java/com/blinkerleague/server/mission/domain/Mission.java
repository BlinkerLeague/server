package com.blinkerleague.server.mission.domain;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 미션 정의(plan.md 3.6). active_from ~ active_to(포함) 동안 운영한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

	private Long id;
	private String title;
	private TurnSignalCondition condition;
	private int rewardPoint;
	private LocalDate activeFrom;
	private LocalDate activeTo;

	private Mission(String title, TurnSignalCondition condition, int rewardPoint, LocalDate activeFrom,
		LocalDate activeTo) {
		this.title = title;
		this.condition = condition;
		this.rewardPoint = rewardPoint;
		this.activeFrom = activeFrom;
		this.activeTo = activeTo;
	}

	public static Mission daily(String title, TurnSignalCondition condition, int rewardPoint, LocalDate activeFrom,
		LocalDate activeTo) {
		return new Mission(title, condition, rewardPoint, activeFrom, activeTo);
	}
}
