package com.blinkerleague.server.mission.adapter.out.persistence;

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
import com.blinkerleague.server.mission.domain.DriverMission;
import com.blinkerleague.server.mission.domain.MissionStatus;

@DataJpaTest
@ActiveProfiles("test")
@Import({DriverMissionPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class DriverMissionPersistenceAdapterTest {

	private static final Instant ACHIEVED_AT = Instant.parse("2026-11-03T00:40:00Z");

	@Autowired
	DriverMissionPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 운전자_미션_날짜로_저장한_기록을_다시_읽는다() {
		DriverMission record = DriverMission.start(1L, 10L, "2026-11-03");
		record.apply(MissionStatus.ACHIEVED, ACHIEVED_AT);
		adapter.save(record);
		adapter.save(DriverMission.start(1L, 10L, "2026-11-04"));
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.find(1L, 10L, "2026-11-03")).hasValueSatisfying(found -> {
			assertThat(found.getStatus()).isEqualTo(MissionStatus.ACHIEVED);
			assertThat(found.getAchievedAt()).isEqualTo(ACHIEVED_AT);
		});
		assertThat(adapter.find(2L, 10L, "2026-11-03")).isEmpty();
	}
}
