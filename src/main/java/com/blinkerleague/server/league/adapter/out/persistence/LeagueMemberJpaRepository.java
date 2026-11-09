package com.blinkerleague.server.league.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.league.domain.LeagueMember;

public interface LeagueMemberJpaRepository extends JpaRepository<LeagueMember, Long> {

	Optional<LeagueMember> findBySeasonIdAndDriverId(String seasonId, Long driverId);
}
