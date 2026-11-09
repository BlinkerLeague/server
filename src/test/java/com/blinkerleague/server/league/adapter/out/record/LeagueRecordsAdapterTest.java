package com.blinkerleague.server.league.adapter.out.record;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.driver.domain.Driver;
import com.blinkerleague.server.league.domain.LeagueMember;
import com.blinkerleague.server.league.domain.LeagueSeason;
import com.blinkerleague.server.league.domain.MemberRecord;
import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.threshold.domain.ScoreItem;
import com.blinkerleague.server.trip.domain.Trip;

@DataJpaTest
@ActiveProfiles("test")
@Import({LeagueRecordsAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class LeagueRecordsAdapterTest {

	private static final LeagueSeason NOVEMBER = LeagueSeason.containing(Instant.parse("2026-11-10T00:00:00Z"),
		TimeConfig.ZONE);

	@Autowired
	LeagueRecordsAdapter adapter;

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void 시즌_소속_운전자마다_시즌_안_참여_운행의_수_거리와_리그_점수_거리_가중_합을_읽는다() {
		Long me = driver("me", "11440");
		Long hidden = driver("hidden", "11410");
		Long notMember = driver("other", "11440");
		member(me, "SEOUL_NORTHWEST");
		member(hidden, "SEOUL_NORTHWEST");
		Instant inSeason = NOVEMBER.startsAt().plusSeconds(86_400);
		trip("t1", me, inSeason, 10.0, 90);
		trip("t2", me, inSeason.plusSeconds(3600), 30.0, 70);
		trip("t3-no-league", me, inSeason.plusSeconds(7200), 5.0, null);
		trip("t4-short", me, inSeason.plusSeconds(9000), 0.5, null);
		trip("t5-before", me, NOVEMBER.startsAt().minusSeconds(1), 40.0, 10);
		trip("t6-next", me, NOVEMBER.endsAt(), 40.0, 10);
		trip("h1", hidden, inSeason, 20.0, 80);
		trip("o1", notMember, inSeason, 20.0, 80);
		entityManager.flush();
		jdbcTemplate.update("update driver set league_visible = false where driver_id = ?", hidden);

		List<MemberRecord> records = adapter.records(NOVEMBER);

		assertThat(records).containsExactlyInAnyOrder(
			new MemberRecord(me, "me", "SEOUL_NORTHWEST", true, 3, 45.0, 40.0, 90 * 10 + 70 * 30, 0),
			new MemberRecord(hidden, "hidden", "SEOUL_NORTHWEST", false, 1, 20.0, 20.0, 80 * 20, 0));
	}

	@Test
	void 시즌_소속_운전자마다_시즌_안에_적립한_포인트_합을_읽는다() {
		Long me = driver("me", "11440");
		Long other = driver("other", "11440");
		member(me, "SEOUL_NORTHWEST");
		member(other, "SEOUL_NORTHWEST");
		Instant inSeason = NOVEMBER.startsAt().plusSeconds(86_400);
		trip("t1", me, inSeason, 10.0, 90);
		entityManager.persist(PointEntry.trip(me, "t1", 9, 10.0, 90, inSeason));
		entityManager.persist(PointEntry.dailyMission(me, "m1", 30, inSeason.plusSeconds(60)));
		entityManager.persist(PointEntry.streak(me, "s1", 50, NOVEMBER.endsAt().minusSeconds(1)));
		// 시즌 밖에 적립한 포인트와 다른 운전자의 포인트는 세지 않는다
		entityManager.persist(PointEntry.dailyMission(me, "m0", 30, NOVEMBER.startsAt().minusSeconds(1)));
		entityManager.persist(PointEntry.dailyMission(me, "m2", 30, NOVEMBER.endsAt()));
		entityManager.persist(PointEntry.dailyMission(other, "o1", 30, inSeason));
		entityManager.flush();

		List<MemberRecord> records = adapter.records(NOVEMBER);

		assertThat(records).filteredOn(record -> record.driverId().equals(me))
			.singleElement().extracting(MemberRecord::seasonPoints).isEqualTo(9 + 30 + 50);
	}

	private Long driver(String nickname, String regionCode) {
		return entityManager.persistAndFlush(Driver.register(nickname, regionCode)).getId();
	}

	private void member(Long driverId, String leagueCode) {
		entityManager.persist(new LeagueMember(NOVEMBER.id(), driverId, leagueCode));
	}

	// leagueScore가 null이면 공통 항목을 측정하지 않은 운행, 거리 1km 미만이면 EXCLUDED_SHORT
	private void trip(String tripId, Long driverId, Instant endedAt, double distanceKm, Integer leagueScore) {
		Trip trip = Trip.upload(tripId, driverId, endedAt.minusSeconds(1800), endedAt, distanceKm,
			Set.of(ScoreItem.TURN_SIGNAL), List.of(), 1.0);
		if (distanceKm >= 1.0) {
			trip.applyScore(80, List.of());
			trip.applyLeagueScore(leagueScore);
		}
		entityManager.persist(trip);
	}
}
