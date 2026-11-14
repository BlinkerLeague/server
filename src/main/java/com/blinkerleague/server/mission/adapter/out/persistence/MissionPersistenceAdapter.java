package com.blinkerleague.server.mission.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.SaveMissionPort;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionPeriod;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionPersistenceAdapter implements LoadActiveMissionPort, SaveMissionPort {

	private final MissionJpaRepository repository;

	@Override
	public Optional<Mission> findActiveDaily(LocalDate date) {
		// P0은 일일 미션이 하나다. 여럿이 겹치면 먼저 만든 미션을 쓴다.
		return repository.findFirstByPeriodAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByIdAsc(
			MissionPeriod.DAILY, date, date);
	}

	@Override
	public boolean existsAny() {
		return repository.count() > 0;
	}

	@Override
	public void saveAll(List<Mission> missions) {
		repository.saveAll(missions);
	}
}
