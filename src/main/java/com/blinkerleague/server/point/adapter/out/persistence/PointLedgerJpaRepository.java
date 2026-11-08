package com.blinkerleague.server.point.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

interface PointLedgerJpaRepository extends JpaRepository<PointEntry, Long> {

	boolean existsByReasonAndRefId(PointReason reason, String refId);

	@Query("select coalesce(sum(e.amount), 0) from PointEntry e where e.driverId = :driverId")
	int sumAmount(@Param("driverId") Long driverId);

	@Query("""
		select coalesce(sum(e.amount), 0) from PointEntry e
		where e.driverId = :driverId and e.earnedAt >= :from and e.earnedAt < :to
		""")
	int sumAmount(@Param("driverId") Long driverId, @Param("from") Instant from, @Param("to") Instant to);
}
