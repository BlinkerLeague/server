package com.blinkerleague.server.league.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.domain.LeagueMember;

@DataJpaTest
@ActiveProfiles("test")
@Import({LeagueMemberPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class LeagueMemberPersistenceAdapterTest {

	@Autowired
	LeagueMemberPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 시즌_소속을_저장하고_같은_운전자_시즌을_또_저장하면_처음_소속을_유지한다() {
		adapter.saveIfAbsent(new LeagueMember("2026-11", 1L, "SEOUL_NORTHWEST"));
		adapter.saveIfAbsent(new LeagueMember("2026-11", 1L, "BUSAN"));
		adapter.saveIfAbsent(new LeagueMember("2026-12", 1L, "BUSAN"));
		entityManager.clear();

		assertThat(adapter.find("2026-11", 1L)).map(LeagueMember::getLeagueCode).contains("SEOUL_NORTHWEST");
		assertThat(adapter.find("2026-12", 1L)).map(LeagueMember::getLeagueCode).contains("BUSAN");
		assertThat(adapter.find("2026-10", 1L)).isEmpty();
	}
}
