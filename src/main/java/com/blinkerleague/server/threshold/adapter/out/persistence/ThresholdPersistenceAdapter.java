package com.blinkerleague.server.threshold.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** 기준값을 `app_config`의 `thresholds` 행에 JSON으로 저장한다. */
@Component
@RequiredArgsConstructor
public class ThresholdPersistenceAdapter implements LoadThresholdsPort, SaveThresholdsPort {

	static final String KEY = "thresholds";

	private final AppConfigJpaRepository repository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public Optional<VersionedThresholds> load() {
		return repository.findById(KEY)
			.map(entity -> new VersionedThresholds(entity.getVersion(), fromJson(entity.getValue())));
	}

	@Override
	@Transactional
	public void save(VersionedThresholds thresholds) {
		String json = toJson(thresholds.thresholds());
		repository.findById(KEY).ifPresentOrElse(
			entity -> entity.change(json, thresholds.version()),
			() -> repository.save(new AppConfigEntity(KEY, json, thresholds.version())));
	}

	private String toJson(Thresholds thresholds) {
		try {
			return objectMapper.writeValueAsString(thresholds);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("기준값을 JSON으로 바꾸지 못했습니다.", e);
		}
	}

	private Thresholds fromJson(String json) {
		try {
			return objectMapper.readValue(json, Thresholds.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("저장된 기준값 JSON을 읽지 못했습니다.", e);
		}
	}
}
