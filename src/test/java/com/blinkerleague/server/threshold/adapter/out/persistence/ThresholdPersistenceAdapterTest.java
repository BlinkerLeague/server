package com.blinkerleague.server.threshold.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.threshold.ThresholdsFixture;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

@DataJpaTest
@ActiveProfiles("test")
@Import({ThresholdPersistenceAdapter.class, JacksonAutoConfiguration.class, JpaAuditingConfig.class,
	TimeConfig.class})
class ThresholdPersistenceAdapterTest {

	@Autowired
	ThresholdPersistenceAdapter adapter;

	@Autowired
	AppConfigJpaRepository repository;

	@Test
	void 저장한_기준값을_그대로_읽고_다시_저장하면_한_행을_덮어쓴다() {
		assertThat(adapter.load()).isEmpty();

		VersionedThresholds first = new VersionedThresholds(1, ThresholdsFixture.planExample());
		adapter.save(first);
		assertThat(adapter.load()).contains(first);

		VersionedThresholds second = new VersionedThresholds(2, ThresholdsFixture.withMinDistanceKm(2.0));
		adapter.save(second);
		assertThat(adapter.load()).contains(second);
		assertThat(repository.count()).isEqualTo(1);
	}
}
