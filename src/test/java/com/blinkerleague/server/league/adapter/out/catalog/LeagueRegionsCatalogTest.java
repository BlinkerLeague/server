package com.blinkerleague.server.league.adapter.out.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.league.domain.LeagueRegion;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.fasterxml.jackson.databind.ObjectMapper;

class LeagueRegionsCatalogTest {

	private final LeagueRegions regions = new LeagueRegionsCatalog(new ObjectMapper()).load();

	@Test
	void 서울_25개_구는_생활권_5개로_묶는다() {
		Map<String, String> expected = Map.ofEntries(
			Map.entry("11110", "SEOUL_CENTRAL"), Map.entry("11140", "SEOUL_CENTRAL"), Map.entry("11170", "SEOUL_CENTRAL"),
			Map.entry("11200", "SEOUL_NORTHEAST"), Map.entry("11215", "SEOUL_NORTHEAST"), Map.entry("11230", "SEOUL_NORTHEAST"),
			Map.entry("11260", "SEOUL_NORTHEAST"), Map.entry("11290", "SEOUL_NORTHEAST"), Map.entry("11305", "SEOUL_NORTHEAST"),
			Map.entry("11320", "SEOUL_NORTHEAST"), Map.entry("11350", "SEOUL_NORTHEAST"),
			Map.entry("11380", "SEOUL_NORTHWEST"), Map.entry("11410", "SEOUL_NORTHWEST"), Map.entry("11440", "SEOUL_NORTHWEST"),
			Map.entry("11470", "SEOUL_SOUTHWEST"), Map.entry("11500", "SEOUL_SOUTHWEST"), Map.entry("11530", "SEOUL_SOUTHWEST"),
			Map.entry("11545", "SEOUL_SOUTHWEST"), Map.entry("11560", "SEOUL_SOUTHWEST"), Map.entry("11590", "SEOUL_SOUTHWEST"),
			Map.entry("11620", "SEOUL_SOUTHWEST"),
			Map.entry("11650", "SEOUL_SOUTHEAST"), Map.entry("11680", "SEOUL_SOUTHEAST"), Map.entry("11710", "SEOUL_SOUTHEAST"),
			Map.entry("11740", "SEOUL_SOUTHEAST"));

		expected.forEach((sigungu, league) ->
			assertThat(regions.regionOf(sigungu)).as(sigungu).map(LeagueRegion::code).contains(league));
	}

	@Test
	void 서울_밖은_시도_단위로_묶고_옛_코드와_새_특별자치도_코드를_모두_받는다() {
		assertThat(code("26440")).isEqualTo("BUSAN");
		assertThat(code("41135")).isEqualTo("GYEONGGI");
		assertThat(code("42110")).isEqualTo("GANGWON");
		assertThat(code("51110")).isEqualTo("GANGWON");
		assertThat(code("45111")).isEqualTo("JEONBUK");
		assertThat(code("52111")).isEqualTo("JEONBUK");
		assertThat(code("50110")).isEqualTo("JEJU");
		assertThat(regions.regionOf("99999")).isEmpty();
		assertThat(regions.all()).hasSize(21);
		assertThat(Stream.of("11999", "26999").map(regions::regionOf)).allMatch(java.util.Optional::isPresent);
	}

	private String code(String sigungu) {
		return regions.regionOf(sigungu).map(LeagueRegion::code).orElse(null);
	}
}
