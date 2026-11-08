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

	@Test
	void 운전자의_기간_안_적립만_합산하고_끝_시각은_포함하지_않는다() {
		Instant from = Instant.parse("2026-11-02T15:00:00Z");
		Instant to = Instant.parse("2026-11-03T15:00:00Z");
		adapter.saveNew(PointEntry.trip(1L, "before", 100, 50.0, 100, from.minusSeconds(1)));
		adapter.saveNew(PointEntry.trip(1L, "start", 10, 12.3, 86, from));
		adapter.saveNew(PointEntry.trip(1L, "inside", 20, 20.0, 100, NOW));
		adapter.saveNew(PointEntry.trip(1L, "end", 100, 50.0, 100, to));
		adapter.saveNew(PointEntry.trip(2L, "other", 100, 50.0, 100, NOW));

		assertThat(adapter.sumEarned(1L, from, to)).isEqualTo(30);
		assertThat(adapter.sumEarned(3L, from, to)).isZero();
	}
}
