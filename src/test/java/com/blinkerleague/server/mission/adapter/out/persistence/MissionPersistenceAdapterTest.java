package com.blinkerleague.server.mission.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.TurnSignalCondition;

@DataJpaTest
@ActiveProfiles("test")
@Import({MissionPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class MissionPersistenceAdapterTest {

	@Autowired
	MissionPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 운영_기간_양_끝을_포함해_그날의_일일_미션을_찾는다() {
		entityManager.persist(Mission.daily("회전 깜빡이 완벽", new TurnSignalCondition(3, 100), 20,
			LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findActiveDaily(LocalDate.of(2026, 10, 31))).isEmpty();
		assertThat(adapter.findActiveDaily(LocalDate.of(2026, 11, 1))).get()
			.extracting(Mission::getTitle, Mission::getCondition, Mission::getRewardPoint)
			.containsExactly("회전 깜빡이 완벽", new TurnSignalCondition(3, 100), 20);
		assertThat(adapter.findActiveDaily(LocalDate.of(2026, 11, 30))).isPresent();
		assertThat(adapter.findActiveDaily(LocalDate.of(2026, 12, 1))).isEmpty();
	}
}
