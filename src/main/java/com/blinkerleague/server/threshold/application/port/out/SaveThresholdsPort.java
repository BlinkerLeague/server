package com.blinkerleague.server.threshold.application.port.out;

import com.blinkerleague.server.threshold.domain.VersionedThresholds;

public interface SaveThresholdsPort {

	/** 기준값을 저장한다. 이미 있으면 덮어쓴다. */
	void save(VersionedThresholds thresholds);
}
