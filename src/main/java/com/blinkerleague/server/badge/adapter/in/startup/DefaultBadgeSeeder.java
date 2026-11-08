package com.blinkerleague.server.badge.adapter.in.startup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.badge.application.service.SeedBadgeService;
import com.blinkerleague.server.badge.domain.Badge;
import com.blinkerleague.server.badge.domain.BadgeRule;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 시작할 때 저장된 배지가 없으면 `badges/default-badges.json`의 카탈로그를 넣는다.
 * 웹 서버가 요청을 받기 전에 끝나도록 모든 싱글톤 생성 직후에 실행한다.
 */
@Component
@RequiredArgsConstructor
public class DefaultBadgeSeeder implements SmartInitializingSingleton {

	static final String DEFAULTS_PATH = "badges/default-badges.json";

	private final SeedBadgeService seedBadgeService;
	private final ObjectMapper objectMapper;

	@Override
	public void afterSingletonsInstantiated() {
		seedBadgeService.seedIfAbsent(readDefaults());
	}

	private List<Badge> readDefaults() {
		try (InputStream in = new ClassPathResource(DEFAULTS_PATH).getInputStream()) {
			return Arrays.stream(objectMapper.readValue(in, DefaultBadge[].class))
				.map(DefaultBadge::toBadge)
				.toList();
		} catch (IOException e) {
			throw new IllegalStateException("기본 배지 파일을 읽지 못했습니다: " + DEFAULTS_PATH, e);
		}
	}

	record DefaultBadge(String code, String name, String condition, BadgeRule rule, int target, int sortOrder) {

		Badge toBadge() {
			return new Badge(code, name, condition, rule, target, sortOrder);
		}
	}
}
