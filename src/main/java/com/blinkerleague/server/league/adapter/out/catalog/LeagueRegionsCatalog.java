package com.blinkerleague.server.league.adapter.out.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.port.out.LoadLeagueRegionsPort;
import com.blinkerleague.server.league.domain.LeagueRegion;
import com.blinkerleague.server.league.domain.LeagueRegions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/** {@code leagues/league-regions.json}의 매핑표. 리그를 나누거나 합칠 때는 이 파일만 바꾼다(D11). */
@Component
public class LeagueRegionsCatalog implements LoadLeagueRegionsPort {

	private static final String LOCATION = "leagues/league-regions.json";

	private final LeagueRegions regions;

	public LeagueRegionsCatalog(ObjectMapper objectMapper) {
		this.regions = read(objectMapper);
	}

	@Override
	public LeagueRegions load() {
		return regions;
	}

	private static LeagueRegions read(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
			Catalog catalog = objectMapper.readValue(in, Catalog.class);
			return new LeagueRegions(catalog.regions().stream()
				.map(entry -> new LeagueRegions.Rule(new LeagueRegion(entry.code(), entry.name()),
					entry.sigungu() == null ? List.of() : entry.sigungu(),
					entry.sidoPrefixes() == null ? List.of() : entry.sidoPrefixes()))
				.toList());
		} catch (IOException e) {
			throw new UncheckedIOException("리그 권역 매핑표를 읽을 수 없습니다: " + LOCATION, e);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Catalog(List<Entry> regions) {
	}

	private record Entry(String code, String name, List<String> sigungu, List<String> sidoPrefixes) {
	}
}
