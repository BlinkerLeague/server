package com.blinkerleague.server.badge.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blinkerleague.server.badge.application.port.out.BadgeAwardStore;
import com.blinkerleague.server.badge.application.port.out.LoadBadgeCatalogPort;
import com.blinkerleague.server.badge.domain.BadgeAward;

import lombok.RequiredArgsConstructor;

/** 배지 진열장 조회(plan.md 4.3, SCR-05 GET /api/badges). 받지 않은 배지도 잠금 상태로 모두 보여 준다. */
@RequiredArgsConstructor
public class GetBadgesService {

	private final LoadBadgeCatalogPort loadBadgeCatalogPort;
	private final BadgeAwardStore badgeAwardStore;

	public List<BadgeView> get(Long driverId) {
		Map<String, Instant> awarded = badgeAwardStore.findByDriver(driverId).stream()
			.collect(Collectors.toMap(BadgeAward::getBadgeCode, BadgeAward::getAwardedAt));
		return loadBadgeCatalogPort.findAll().stream()
			.map(badge -> new BadgeView(badge.getCode(), badge.getName(), badge.getCondition(),
				awarded.containsKey(badge.getCode()), awarded.get(badge.getCode())))
			.toList();
	}

	/** awardedAt은 받지 않았으면 null이다. */
	public record BadgeView(String code, String name, String condition, boolean earned, Instant awardedAt) {
	}
}
