package com.blinkerleague.server.league.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LeagueRegionsTest {

	private final LeagueRegion seoulNorthwest = new LeagueRegion("SEOUL_NORTHWEST", "서울 서북권");
	private final LeagueRegion seoulCentral = new LeagueRegion("SEOUL_CENTRAL", "서울 도심권");
	private final LeagueRegion busan = new LeagueRegion("BUSAN", "부산");

	private final LeagueRegions regions = new LeagueRegions(List.of(
		new LeagueRegions.Rule(seoulCentral, List.of("11110"), List.of()),
		new LeagueRegions.Rule(seoulNorthwest, List.of("11380", "11410", "11440"), List.of()),
		new LeagueRegions.Rule(busan, List.of(), List.of("26"))));

	@Test
	void 시군구_코드는_매핑표의_시군구가_먼저고_없으면_시도_앞자리로_리그_권역을_찾는다() {
		assertThat(regions.regionOf("11440")).contains(seoulNorthwest);
		assertThat(regions.regionOf("11110")).contains(seoulCentral);
		assertThat(regions.regionOf("26440")).contains(busan);
		assertThat(regions.regionOf("99999")).isEmpty();
		assertThat(regions.all()).containsExactly(seoulCentral, seoulNorthwest, busan);
	}
}
