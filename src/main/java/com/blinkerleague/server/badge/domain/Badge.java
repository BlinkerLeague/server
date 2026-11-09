package com.blinkerleague.server.badge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 배지 카탈로그 항목(plan.md 4.3, SCR-05 진열장). 획득 여부와 관계없이 전체를 보여 준다. */
@Getter
@Entity
@Table(name = "badge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

	@Id
	@Column(length = 30)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "condition_text", nullable = false, length = 100)
	private String condition;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BadgeRule rule;

	@Column(nullable = false)
	private int target;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public Badge(String code, String name, String condition, BadgeRule rule, int target, int sortOrder) {
		this.code = code;
		this.name = name;
		this.condition = condition;
		this.rule = rule;
		this.target = target;
		this.sortOrder = sortOrder;
	}

	public boolean isEarnedBy(BadgeProgress progress) {
		int value = switch (rule) {
			case SCORED_TRIPS -> progress.scoredTrips();
			case STREAK_DAYS -> progress.longestStreakDays();
			case SIGNAL_ON_TURNS -> progress.signalOnTurns();
			case SEASON_CHAMPIONS -> 0;
		};
		return value >= target;
	}
}
