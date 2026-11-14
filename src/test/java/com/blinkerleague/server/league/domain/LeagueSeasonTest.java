package com.blinkerleague.server.league.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.blinkerleague.server.common.time.TimeConfig;

class LeagueSeasonTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
		"2026-11-01T00:00:00+09:00, 2026-11, 2026-11-01T00:00+09:00, 2026-12-01T00:00+09:00",
		"2026-11-30T23:59:59+09:00, 2026-11, 2026-11-01T00:00+09:00, 2026-12-01T00:00+09:00",
		"2026-11-30T15:00:00Z,      2026-12, 2026-12-01T00:00+09:00, 2027-01-01T00:00+09:00"})
	void 시즌은_한국_시간_달력_월_단위다(OffsetDateTime at, String id, OffsetDateTime startsAt, OffsetDateTime endsAt) {
		LeagueSeason season = LeagueSeason.containing(at.toInstant(), TimeConfig.ZONE);

		assertThat(season.id()).isEqualTo(id);
		assertThat(season.startsAt()).isEqualTo(startsAt.toInstant());
		assertThat(season.endsAt()).isEqualTo(endsAt.toInstant());
	}
}
