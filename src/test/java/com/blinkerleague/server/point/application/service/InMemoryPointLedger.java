package com.blinkerleague.server.point.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.blinkerleague.server.point.application.port.out.LoadPointLedgerPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.application.port.out.SavePointEntryPort.PointAlreadyEarnedException;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

/** 포인트 원장 포트의 메모리 구현. 서비스 단위 테스트용. */
class InMemoryPointLedger implements LoadPointLedgerPort, SavePointEntryPort {

	private final List<PointEntry> entries = new ArrayList<>();
	private boolean nextSaveConflicts;

	@Override
	public void saveNew(PointEntry entry) {
		if (nextSaveConflicts) {
			nextSaveConflicts = false;
			throw new PointAlreadyEarnedException(entry.getReason(), entry.getRefId(), null);
		}
		entries.add(entry);
	}

	/** 확인과 저장 사이에 다른 요청이 같은 적립을 먼저 저장한 상황을 흉내 낸다. */
	void conflictOnNextSave() {
		nextSaveConflicts = true;
	}

	@Override
	public boolean exists(PointReason reason, String refId) {
		return entries.stream().anyMatch(entry -> entry.getReason() == reason && entry.getRefId().equals(refId));
	}

	@Override
	public int sumEarned(Long driverId, Instant from, Instant to) {
		return entries.stream()
			.filter(entry -> entry.getDriverId().equals(driverId))
			.filter(entry -> !entry.getEarnedAt().isBefore(from) && entry.getEarnedAt().isBefore(to))
			.mapToInt(PointEntry::getAmount)
			.sum();
	}

	List<PointEntry> entries() {
		return List.copyOf(entries);
	}
}
