package com.blinkerleague.server.league.domain;

import java.util.List;
import java.util.Optional;

/**
 * 시군구 → 리그 권역 매핑표(D11, 초기 묶음은 D31). 권역마다 시군구 코드 목록과 시 · 도 앞 2자리 목록을 둔다.
 * 매핑만 바꾸면 리그를 나누거나 합칠 수 있고, 리그를 시군구 코드로 직접 나누지 않는다.
 */
public record LeagueRegions(List<Rule> rules) {

	public Optional<LeagueRegion> regionOf(String regionCode) {
		throw new UnsupportedOperationException();
	}

	public List<LeagueRegion> all() {
		throw new UnsupportedOperationException();
	}

	public record Rule(LeagueRegion region, List<String> sigungu, List<String> sidoPrefixes) {
	}
}
