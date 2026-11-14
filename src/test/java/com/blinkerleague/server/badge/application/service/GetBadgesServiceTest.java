package com.blinkerleague.server.badge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.badge.application.service.GetBadgesService.BadgeView;
import com.blinkerleague.server.badge.domain.BadgeAward;

class GetBadgesServiceTest {

	private static final Instant AWARDED_AT = Instant.parse("2026-11-03T03:00:00Z");

	@Test
	void 전체_배지를_진열_순서로_돌려주고_받은_배지는_받은_시각을_담는다() {
		InMemoryBadgeAwardStore store = new InMemoryBadgeAwardStore();
		store.saveIfAbsent(new BadgeAward(1L, "STREAK_7", AWARDED_AT));
		store.saveIfAbsent(new BadgeAward(2L, "FIRST_TRIP", AWARDED_AT));
		GetBadgesService service = new GetBadgesService(() -> AwardBadgesServiceTest.CATALOG, store);

		assertThat(service.get(1L)).containsExactly(
			new BadgeView("FIRST_TRIP", "첫 경기", "점수가 난 첫 운행", false, null),
			new BadgeView("STREAK_7", "7일 연속", "미션 7일 연속 달성", true, AWARDED_AT),
			new BadgeView("SIGNAL_100", "깜빡이 100회", "깜빡이를 켠 회전 100회", false, null));
	}
}
