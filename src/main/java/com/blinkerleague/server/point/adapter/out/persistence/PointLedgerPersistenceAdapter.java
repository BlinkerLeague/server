package com.blinkerleague.server.point.adapter.out.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PointLedgerPersistenceAdapter implements LoadPointLedgerPort, SavePointEntryPort {

	private final PointLedgerJpaRepository repository;

	@Override
	public boolean exists(PointReason reason, String refId) {
		return repository.existsByReasonAndRefId(reason, refId);
	}

	@Override
	public int sumEarned(Long driverId, Instant from, Instant to) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveNew(PointEntry entry) {
		repository.saveAndFlush(entry);
	}
}
