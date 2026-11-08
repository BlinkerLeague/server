package com.blinkerleague.server.mission.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore.DriverMissionAlreadyExistsException;
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

	@Test
	void 같은_운전자_미션_날짜의_새_기록을_또_저장하면_이미_있다고_알린다() {
		adapter.save(DriverMission.start(1L, 10L, "2026-11-03"));

		assertThatThrownBy(() -> adapter.save(DriverMission.start(1L, 10L, "2026-11-03")))
			.isInstanceOf(DriverMissionAlreadyExistsException.class);
	}

	@Test
	void 주어진_날보다_앞선_날_중_가장_최근_기록을_찾는다() {
		adapter.save(DriverMission.start(1L, 10L, "2026-10-30"));
		adapter.save(DriverMission.start(1L, 10L, "2026-11-01"));
		adapter.save(DriverMission.start(1L, 10L, "2026-11-03"));
		adapter.save(DriverMission.start(2L, 10L, "2026-11-02"));

		assertThat(adapter.findLatestBefore(1L, "2026-11-03"))
			.hasValueSatisfying(found -> assertThat(found.getPeriodKey()).isEqualTo("2026-11-01"));
		assertThat(adapter.findLatestBefore(1L, "2026-10-30")).isEmpty();
	}

	@Test
	void 운전자가_달성한_날들만_읽는다() {
		DriverMission achieved = DriverMission.start(1L, 10L, "2026-11-02");
		achieved.apply(MissionStatus.ACHIEVED, ACHIEVED_AT);
		adapter.save(achieved);
		DriverMission failed = DriverMission.start(1L, 10L, "2026-11-03");
		failed.apply(MissionStatus.FAILED, ACHIEVED_AT);
		adapter.save(failed);
		adapter.save(DriverMission.start(1L, 10L, "2026-11-04"));
		DriverMission other = DriverMission.start(2L, 10L, "2026-11-04");
		other.apply(MissionStatus.ACHIEVED, ACHIEVED_AT);
		adapter.save(other);

		assertThat(adapter.achievedDays(1L)).containsExactly(java.time.LocalDate.of(2026, 11, 2));
	}
}
