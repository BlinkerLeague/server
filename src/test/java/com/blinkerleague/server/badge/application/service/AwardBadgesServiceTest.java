package com.blinkerleague.server.badge.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.badge.domain.Badge;
import com.blinkerleague.server.badge.domain.BadgeAward;
import com.blinkerleague.server.badge.domain.BadgeProgress;
import com.blinkerleague.server.badge.domain.BadgeRule;

class AwardBadgesServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");

	static final List<Badge> CATALOG = List.of(
		new Badge("FIRST_TRIP", "첫 경기", "점수가 난 첫 운행", BadgeRule.SCORED_TRIPS, 1, 1),
		new Badge("STREAK_7", "7일 연속", "미션 7일 연속 달성", BadgeRule.STREAK_DAYS, 7, 2),
		new Badge("SIGNAL_100", "깜빡이 100회", "깜빡이를 켠 회전 100회", BadgeRule.SIGNAL_ON_TURNS, 100, 3));

	private final InMemoryBadgeAwardStore store = new InMemoryBadgeAwardStore();

	@Test
	void 기록이_조건에_닿았고_아직_받지_않은_배지만_새로_준다() {
		store.saveIfAbsent(new BadgeAward(1L, "FIRST_TRIP", NOW.minusSeconds(86_400)));
		AwardBadgesService service = new AwardBadgesService(() -> CATALOG,
			driverId -> new BadgeProgress(5, 7, 40, 0), store, Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));

		List<String> awarded = service.awardEarned(1L);

		assertThat(awarded).containsExactly("STREAK_7");
		assertThat(store.saved).extracting(BadgeAward::getBadgeCode).containsExactly("FIRST_TRIP", "STREAK_7");
		assertThat(store.saved.get(1).getAwardedAt()).isEqualTo(NOW);
	}
}
