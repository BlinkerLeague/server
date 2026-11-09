package com.blinkerleague.server.league.domain;

import java.util.List;
import java.util.Optional;

/**
 * 시군구 → 리그 권역 매핑표(D11, 초기 묶음은 D31). 권역마다 시군구 코드 목록과 시 · 도 앞 2자리 목록을 둔다.
 * 매핑만 바꾸면 리그를 나누거나 합칠 수 있고, 리그를 시군구 코드로 직접 나누지 않는다.
 */
public record LeagueRegions(List<Rule> rules) {

	public Optional<LeagueRegion> regionOf(String regionCode) {
		Optional<LeagueRegion> exact = rules.stream()
			.filter(rule -> rule.sigungu().contains(regionCode))
			.map(Rule::region)
			.findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		return rules.stream()
			.filter(rule -> rule.sidoPrefixes().stream().anyMatch(regionCode::startsWith))
			.map(Rule::region)
			.findFirst();
	}

	/** 매핑표 순서대로의 모든 권역(월드맵 목록). */
	public List<LeagueRegion> all() {
		return rules.stream().map(Rule::region).toList();
	}

	public record Rule(LeagueRegion region, List<String> sigungu, List<String> sidoPrefixes) {
	}
}
