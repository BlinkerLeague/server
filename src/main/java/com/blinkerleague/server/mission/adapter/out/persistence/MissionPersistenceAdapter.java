package com.blinkerleague.server.mission.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.domain.Mission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionPersistenceAdapter implements LoadActiveMissionPort {

	private final MissionJpaRepository repository;

	@Override
	public Optional<Mission> findActiveDaily(LocalDate date) {
		throw new UnsupportedOperationException();
	}
}
