package com.blinkerleague.server.point.adapter.out.persistence;

import java.time.Instant;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort.PointAlreadyEarnedException;
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
		return repository.sumAmount(driverId, from, to);
	}

	@Override
	public void saveNew(PointEntry entry) {
		try {
			// 즉시 flush해 동시 적립으로 생긴 (reason, ref_id) 유니크 충돌을 여기서 잡는다.
			repository.saveAndFlush(entry);
		} catch (DataIntegrityViolationException e) {
			if (isUniqueViolation(e)) {
				throw new PointAlreadyEarnedException(entry.getReason(), entry.getRefId(), e);
			}
			throw e;
		}
	}

	private static boolean isUniqueViolation(DataIntegrityViolationException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
