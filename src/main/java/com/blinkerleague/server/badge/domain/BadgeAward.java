package com.blinkerleague.server.badge.domain;

import java.time.Instant;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운전자가 받은 배지(plan.md 4.3 badge_award). 같은 배지는 한 번만 받는다. */
@Getter
@Entity
@Table(name = "badge_award", uniqueConstraints = @UniqueConstraint(name = "uk_badge_award_driver_badge",
	columnNames = {"driver_id", "badge_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeAward extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "badge_award_id")
	private Long id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "badge_code", nullable = false, length = 30)
	private String badgeCode;

	@Column(name = "awarded_at", nullable = false)
	private Instant awardedAt;

	public BadgeAward(Long driverId, String badgeCode, Instant awardedAt) {
		this.driverId = driverId;
		this.badgeCode = badgeCode;
		this.awardedAt = awardedAt;
	}
}
