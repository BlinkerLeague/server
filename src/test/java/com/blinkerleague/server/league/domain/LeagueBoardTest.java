package com.blinkerleague.server.league.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

class LeagueBoardTest {

	private static final LeagueRegion NORTHWEST = new LeagueRegion("SEOUL_NORTHWEST", "서울 서북권");
	private static final LeagueRegion BUSAN = new LeagueRegion("BUSAN", "부산");
	private static final List<LeagueRegion> REGIONS = List.of(NORTHWEST, BUSAN);
	// 테스트마다 한 규칙만 보도록 참여자 · 자격 기준을 낮춘 값
	private static final LeagueBoard.Rules OPEN = new LeagueBoard.Rules(1, 0, 0);

	@Test
	void 운전자_점수는_리그_점수의_거리_가중_평균이고_점수순으로_같은_점수는_같은_순위다() {
		List<MemberRecord> members = List.of(
			// (90 × 10 + 70 × 30) ÷ 40 = 75.0
			member(1L, NORTHWEST, 2, 40, 40, 90 * 10 + 70 * 30),
			member(2L, NORTHWEST, 1, 20, 20, 80 * 20),
			member(3L, NORTHWEST, 1, 20, 20, 75 * 20),
			member(4L, NORTHWEST, 1, 20, 20, 60 * 20));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, OPEN);

		assertThat(board.region("SEOUL_NORTHWEST").rankings())
			.extracting(Ranked::rank, Ranked::driverId, Ranked::score)
			.containsExactly(tuple(1, 2L, 80.0), tuple(2, 1L, 75.0), tuple(2, 3L, 75.0), tuple(4, 4L, 60.0));
		assertThat(board.standingOf(1L)).hasValueSatisfying(standing -> {
			assertThat(standing.score()).isEqualTo(75.0);
			assertThat(standing.rank()).isEqualTo(2);
		});
	}

	@Test
	void 시즌_누적_50km_이상이거나_운행_10회_이상인_운전자만_순위에_오른다() {
		List<MemberRecord> members = List.of(
			member(1L, NORTHWEST, 2, 50, 50, 90 * 50),
			member(2L, NORTHWEST, 10, 30, 30, 80 * 30),
			member(3L, NORTHWEST, 9, 49.9, 49.9, 99 * 49.9));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, new LeagueBoard.Rules(1, 50, 10));

		assertThat(board.region("SEOUL_NORTHWEST").rankings()).extracting(Ranked::driverId).containsExactly(1L, 2L);
		assertThat(board.standingOf(3L)).hasValueSatisfying(standing -> {
			assertThat(standing.eligible()).isFalse();
			assertThat(standing.rank()).isNull();
			assertThat(standing.score()).isEqualTo(99.0);
		});
	}

	static MemberRecord member(Long driverId, LeagueRegion region, int trips, double distanceKm,
		double scoredDistanceKm, double weightedScoreSum) {
		return new MemberRecord(driverId, "driver" + driverId, region.code(), true, trips, distanceKm,
			scoredDistanceKm, weightedScoreSum);
	}
}
