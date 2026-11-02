package com.blinkerleague.server.threshold.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 기준값을 `app_config`의 `thresholds` 행에 JSON으로 저장한다. */
@Component
@RequiredArgsConstructor
public class ThresholdPersistenceAdapter implements LoadThresholdsPort, SaveThresholdsPort {

	private final AppConfigJpaRepository repository;
	private final ObjectMapper objectMapper;

	@Override
	public Optional<VersionedThresholds> load() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void save(VersionedThresholds thresholds) {
		throw new UnsupportedOperationException();
	}
}
