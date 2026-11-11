package com.blinkerleague.server.threshold.application.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;
import com.blinkerleague.server.threshold.application.port.out.LoadThresholdsPort;
import com.blinkerleague.server.threshold.application.port.out.SaveThresholdsPort;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.blinkerleague.server.threshold.domain.ThresholdsValidator;
import com.blinkerleague.server.threshold.domain.VersionedThresholds;

import lombok.RequiredArgsConstructor;

/**
 * 기준값 시드 · 조회 · 변경. 점수 계산마다 읽히므로 현재값은 메모리에 캐시하고, 변경할 때 함께 갱신한다.
 * 캐시는 서버 인스턴스마다 따로 있다. 여러 대로 늘리면 변경 전파가 필요하다.
 */
@RequiredArgsConstructor
public class ThresholdService implements ThresholdProvider {

	private final LoadThresholdsPort loadThresholdsPort;
	private final SaveThresholdsPort saveThresholdsPort;
	private final AtomicReference<VersionedThresholds> cache = new AtomicReference<>();

	/** 저장된 기준값이 없으면 기본값을 version 1로 저장한다. */
	public void seedIfAbsent(Thresholds defaults) {
		Optional<VersionedThresholds> stored = loadThresholdsPort.load();
		if (stored.isPresent()) {
			cache.set(stored.get());
			return;
		}
		save(new VersionedThresholds(1, defaults));
	}

	/** 기준값을 바꾸고 version을 1 올린다. 바뀐 값을 돌려준다. 검증에 어긋나면 저장하지 않고 INVALID_INPUT. */
	public VersionedThresholds update(Thresholds thresholds) {
		List<String> violations = ThresholdsValidator.validate(thresholds);
		if (!violations.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, describe(violations));
		}
		VersionedThresholds updated = new VersionedThresholds(current().version() + 1, thresholds);
		save(updated);
		return updated;
	}

	@Override
	public VersionedThresholds current() {
		return cache.updateAndGet(cached -> cached != null ? cached : loadThresholdsPort.load().orElseThrow());
	}

	private static String describe(List<String> violations) {
		return "기준값이 올바르지 않습니다: " + String.join(" ", violations);
	}

	private void save(VersionedThresholds thresholds) {
		saveThresholdsPort.save(thresholds);
		cache.set(thresholds);
	}
}
