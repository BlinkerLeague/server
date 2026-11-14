package com.blinkerleague.server.badge.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import com.blinkerleague.server.badge.domain.Badge;
import com.blinkerleague.server.badge.domain.BadgeAward;
import com.blinkerleague.server.badge.domain.BadgeRule;
import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import({BadgePersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class BadgePersistenceAdapterTest {

	@Autowired
	BadgePersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 배지_카탈로그를_저장하고_진열_순서로_읽는다() {
		// 테스트 DB에는 시작 시 시드된 기본 배지가 있을 수 있어 새 코드로 확인한다.
		adapter.saveAll(List.of(
			new Badge("T_SECOND", "둘째", "조건", BadgeRule.SCORED_TRIPS, 2, 902),
			new Badge("T_FIRST", "첫째", "조건", BadgeRule.SCORED_TRIPS, 1, 901)));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.existsAny()).isTrue();
		assertThat(adapter.findAll()).extracting(Badge::getCode)
			.containsSubsequence("T_FIRST", "T_SECOND");
	}

	@Test
	void 받은_배지를_저장하고_같은_배지를_또_받으면_무시한다() {
		Instant first = Instant.parse("2026-11-03T03:00:00Z");
		adapter.saveIfAbsent(new BadgeAward(1L, "FIRST_TRIP", first));
		adapter.saveIfAbsent(new BadgeAward(1L, "FIRST_TRIP", first.plusSeconds(60)));
		adapter.saveIfAbsent(new BadgeAward(2L, "FIRST_TRIP", first));
		entityManager.clear();

		assertThat(adapter.findByDriver(1L)).singleElement().satisfies(award -> {
			assertThat(award.getBadgeCode()).isEqualTo("FIRST_TRIP");
			assertThat(award.getAwardedAt()).isEqualTo(first);
		});
	}
}
