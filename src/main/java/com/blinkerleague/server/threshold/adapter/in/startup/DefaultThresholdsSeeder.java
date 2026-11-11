package com.blinkerleague.server.threshold.adapter.in.startup;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.threshold.application.service.ThresholdService;
import com.blinkerleague.server.threshold.domain.Thresholds;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 시작할 때 저장된 기준값이 없으면 `thresholds/default-thresholds.json`을 version 1로 넣는다.
 * 웹 서버가 요청을 받기 전에 끝나도록 모든 싱글톤 생성 직후에 실행한다.
 */
@Component
@RequiredArgsConstructor
public class DefaultThresholdsSeeder implements SmartInitializingSingleton {

	static final String DEFAULTS_PATH = "thresholds/default-thresholds.json";

	private final ThresholdService thresholdService;
	private final ObjectMapper objectMapper;

	@Override
	public void afterSingletonsInstantiated() {
		thresholdService.seedIfAbsent(readDefaults(DEFAULTS_PATH));
	}

	Thresholds readDefaults(String path) {
		try (InputStream in = new ClassPathResource(path).getInputStream()) {
			// 빠진 숫자 필드를 0으로 채우면 검증(0 이상 등)을 통과해 버리므로 빠진 필드는 읽기 실패로 둔다.
			return objectMapper.readerFor(Thresholds.class)
				.with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
					DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
				.readValue(in);
		} catch (IOException e) {
			throw new IllegalStateException("기본 기준값 파일을 읽지 못했습니다: " + path, e);
		}
	}
}
