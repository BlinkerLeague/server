package com.blinkerleague.server.league.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.SeasonChampionStore;
import com.blinkerleague.server.league.domain.SeasonChampion;

import lombok.RequiredArgsConstructor;

/** 시즌 우승 칭호 저장. 정산은 배치 한 곳에서만 돌아 같은 운전자 · 시즌을 미리 찾아보고 건너뛴다. */
@Component
@RequiredArgsConstructor
public class SeasonChampionPersistenceAdapter implements SeasonChampionStore {

	private final SeasonChampionJpaRepository repository;

	@Override
	public void saveIfAbsent(SeasonChampion champion) {
		if (repository.findBySeasonIdAndDriverId(champion.getSeasonId(), champion.getDriverId()).isEmpty()) {
			repository.save(champion);
		}
	}

	@Override
	public List<SeasonChampion> findByDriver(Long driverId) {
		return repository.findByDriverIdOrderBySeasonIdAsc(driverId);
	}
}
