package com.blinkerleague.server.league.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.domain.SeasonChampion;

@DataJpaTest
@ActiveProfiles("test")
@Import({SeasonChampionPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class SeasonChampionPersistenceAdapterTest {

	@Autowired
	SeasonChampionPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 우승_칭호는_운전자_시즌마다_하나이고_오래된_시즌부터_읽는다() {
		adapter.saveIfAbsent(new SeasonChampion("2026-12", 1L, "BUSAN"));
		adapter.saveIfAbsent(new SeasonChampion("2026-11", 1L, "BUSAN"));
		// 정산을 다시 돌려도 같은 시즌 칭호는 하나다
		adapter.saveIfAbsent(new SeasonChampion("2026-11", 1L, "BUSAN"));
		adapter.saveIfAbsent(new SeasonChampion("2026-11", 2L, "BUSAN"));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findByDriver(1L))
			.extracting(SeasonChampion::getSeasonId, SeasonChampion::getLeagueCode)
			.containsExactly(tuple("2026-11", "BUSAN"), tuple("2026-12", "BUSAN"));
	}
}
