package com.blinkerleague.server.badge.application.service;

import java.util.List;

import com.blinkerleague.server.badge.application.port.out.SaveBadgeCatalogPort;
import com.blinkerleague.server.badge.domain.Badge;

import lombok.RequiredArgsConstructor;

/** 시작할 때 기본 배지 카탈로그를 넣는다. 이미 배지가 있으면 건드리지 않는다. */
@RequiredArgsConstructor
public class SeedBadgeService {

	private final SaveBadgeCatalogPort saveBadgeCatalogPort;

	public void seedIfAbsent(List<Badge> defaults) {
		saveBadgeCatalogPort.saveAll(defaults);
	}
}
