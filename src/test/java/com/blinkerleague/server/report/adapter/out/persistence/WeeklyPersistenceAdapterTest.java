package com.blinkerleague.server.report.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.report.domain.WeeklyMission;
import com.blinkerleague.server.report.domain.WeeklyMissionStatus;
import com.blinkerleague.server.threshold.domain.ScoreItem;

@DataJpaTest
@ActiveProfiles("test")
@Import({WeeklyPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class WeeklyPersistenceAdapterTest {

	@Autowired
	WeeklyPersistenceAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void 주간_미션을_저장하고_판정을_반영해_다시_읽는다() {
		WeeklyMission mission = new WeeklyMission(1L, "2026-W46", ScoreItem.SAFETY_WARNING, "안전 경고 없는 주", 100, 50.0);
		adapter.save(mission);
		mission.judge(100, 60.0);
		adapter.save(mission);
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.find(1L, "2026-W46")).hasValueSatisfying(found -> {
			assertThat(found.getMetric()).isEqualTo(ScoreItem.SAFETY_WARNING);
			assertThat(found.getStatus()).isEqualTo(WeeklyMissionStatus.ACHIEVED);
		});
		assertThat(adapter.find(1L, "2026-W47")).isEmpty();
	}

	@Test
	void 주간_리포트_JSON을_운전자_주별로_저장하고_다시_읽는다() {
		adapter.saveReport(1L, "2026-W45", "{\"week\":\"2026-W45\"}");
		entityManager.flush();
		entityManager.clear();

		assertThat(adapter.findReport(1L, "2026-W45")).contains("{\"week\":\"2026-W45\"}");
		assertThat(adapter.findReport(2L, "2026-W45")).isEmpty();
	}
}
