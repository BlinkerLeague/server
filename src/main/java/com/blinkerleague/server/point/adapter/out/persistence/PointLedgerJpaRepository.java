package com.blinkerleague.server.point.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

interface PointLedgerJpaRepository extends JpaRepository<PointEntry, Long> {

	boolean existsByReasonAndRefId(PointReason reason, String refId);
}
