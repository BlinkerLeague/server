package com.blinkerleague.server.point.application.service;

import java.util.ArrayList;
import java.util.List;

import com.blinkerleague.server.point.application.port.out.SavePointEntryPort;
import com.blinkerleague.server.point.domain.PointEntry;

/** 포인트 원장 포트의 메모리 구현. 서비스 단위 테스트용. */
class InMemoryPointLedger implements SavePointEntryPort {

	private final List<PointEntry> entries = new ArrayList<>();

	@Override
	public void saveNew(PointEntry entry) {
		entries.add(entry);
	}

	List<PointEntry> entries() {
		return List.copyOf(entries);
	}
}
