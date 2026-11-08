package com.blinkerleague.server.badge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.badge.application.port.out.SaveBadgeCatalogPort;
import com.blinkerleague.server.badge.domain.Badge;

class SeedBadgeServiceTest {

	private final List<Badge> saved = new ArrayList<>();
	private final SaveBadgeCatalogPort port = new SaveBadgeCatalogPort() {
		@Override
		public boolean existsAny() {
			return !saved.isEmpty();
		}

		@Override
		public void saveAll(List<Badge> badges) {
			saved.addAll(badges);
		}
	};

	@Test
	void 배지가_없으면_기본_카탈로그를_넣는다() {
		new SeedBadgeService(port).seedIfAbsent(AwardBadgesServiceTest.CATALOG);

		assertThat(saved).hasSize(3);
	}

	@Test
	void 이미_배지가_있으면_넣지_않는다() {
		saved.add(AwardBadgesServiceTest.CATALOG.get(0));

		new SeedBadgeService(port).seedIfAbsent(AwardBadgesServiceTest.CATALOG);

		assertThat(saved).hasSize(1);
	}
}
