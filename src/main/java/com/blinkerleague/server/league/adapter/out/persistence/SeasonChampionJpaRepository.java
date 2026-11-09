package com.blinkerleague.server.league.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.league.domain.SeasonChampion;

public interface SeasonChampionJpaRepository extends JpaRepository<SeasonChampion, Long> {

	Optional<SeasonChampion> findBySeasonIdAndDriverId(String seasonId, Long driverId);

	List<SeasonChampion> findByDriverIdOrderBySeasonIdAsc(Long driverId);
}
