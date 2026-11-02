package com.blinkerleague.server.threshold.application.service;

import java.util.Optional;

import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

/** 기준값 저장 포트의 메모리 구현. load 호출 횟수를 센다. */
class InMemoryThresholdStore implements LoadThresholdsPort, SaveThresholdsPort {

	private VersionedThresholds stored;
	private int loadCount;

	@Override
	public Optional<VersionedThresholds> load() {
		loadCount++;
		return Optional.ofNullable(stored);
	}

	@Override
	public void save(VersionedThresholds thresholds) {
		stored = thresholds;
	}

	Optional<VersionedThresholds> stored() {
		return Optional.ofNullable(stored);
	}

	int loadCount() {
		return loadCount;
	}
}
