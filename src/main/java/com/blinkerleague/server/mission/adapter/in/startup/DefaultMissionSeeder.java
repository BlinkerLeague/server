package com.blinkerleague.server.mission.adapter.in.startup;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.mission.application.service.SeedMissionService;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.TurnSignalCondition;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 시작할 때 저장된 미션이 없으면 `missions/default-missions.json`의 일일 미션을 넣는다.
 * 웹 서버가 요청을 받기 전에 끝나도록 모든 싱글톤 생성 직후에 실행한다.
 */
@Component
@RequiredArgsConstructor
public class DefaultMissionSeeder implements SmartInitializingSingleton {

	static final String DEFAULTS_PATH = "missions/default-missions.json";

	private final SeedMissionService seedMissionService;
	private final ObjectMapper objectMapper;

	@Override
	public void afterSingletonsInstantiated() {
		seedMissionService.seedIfAbsent(readDefaults());
	}

	private List<Mission> readDefaults() {
		try (InputStream in = new ClassPathResource(DEFAULTS_PATH).getInputStream()) {
			return Arrays.stream(objectMapper.readValue(in, DefaultMission[].class))
				.map(DefaultMission::toMission)
				.toList();
		} catch (IOException e) {
			throw new IllegalStateException("기본 미션 파일을 읽지 못했습니다: " + DEFAULTS_PATH, e);
		}
	}

	record DefaultMission(String title, TurnSignalCondition condition, int rewardPoint, LocalDate activeFrom,
		LocalDate activeTo) {

		Mission toMission() {
			return Mission.daily(title, condition, rewardPoint, activeFrom, activeTo);
		}
	}
}
