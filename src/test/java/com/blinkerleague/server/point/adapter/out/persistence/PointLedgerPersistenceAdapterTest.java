package com.blinkerleague.server.point.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

@DataJpaTest
@ActiveProfiles("test")
@Import({PointLedgerPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class PointLedgerPersistenceAdapterTest {

	private static final Instant NOW = Instant.parse("2026-11-03T00:40:00Z");

	@Autowired
	PointLedgerPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 저장한_적립을_사유와_refId로_찾는다() {
		adapter.saveNew(PointEntry.trip(1L, "trip-1", 10, 12.3, 86, NOW));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.exists(PointReason.TRIP, "trip-1")).isTrue();
		assertThat(adapter.exists(PointReason.TRIP, "trip-2")).isFalse();
	}
}
