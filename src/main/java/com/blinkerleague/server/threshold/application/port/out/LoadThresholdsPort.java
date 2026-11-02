package com.blinkerleague.server.threshold.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.threshold.domain.VersionedThresholds;

public interface LoadThresholdsPort {

	/** 저장된 기준값. 아직 시드하지 않았으면 비어 있다. */
	Optional<VersionedThresholds> load();
}
