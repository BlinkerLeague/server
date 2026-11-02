package com.blinkerleague.server.threshold.application.service;

import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

import lombok.RequiredArgsConstructor;

/** 기준값 시드 · 조회 · 변경. */
@RequiredArgsConstructor
public class ThresholdService implements ThresholdProvider {

	private final LoadThresholdsPort loadThresholdsPort;
	private final SaveThresholdsPort saveThresholdsPort;

	/** 저장된 기준값이 없으면 기본값을 version 1로 저장한다. */
	public void seedIfAbsent(Thresholds defaults) {
		if (loadThresholdsPort.load().isPresent()) {
			return;
		}
		saveThresholdsPort.save(new VersionedThresholds(1, defaults));
	}

	@Override
	public VersionedThresholds current() {
		return loadThresholdsPort.load().orElseThrow();
	}
}
