package com.blinkerleague.server.league.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

class LeagueBoardTest {

	private static final LeagueRegion NORTHWEST = new LeagueRegion("SEOUL_NORTHWEST", "서울 서북권");
	private static final LeagueRegion BUSAN = new LeagueRegion("BUSAN", "부산");
	private static final LeagueRegion DAEGU = new LeagueRegion("DAEGU", "대구");
	private static final List<LeagueRegion> REGIONS = List.of(NORTHWEST, BUSAN, DAEGU);
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
	void 순위_점수는_리그_점수_거리_가중_평균에_시즌_포인트를_더한_값이다() {
		List<MemberRecord> members = List.of(
			// 평균 90.0 + 포인트 10 = 100.0
			withPoints(member(1L, NORTHWEST, 1, 20, 20, 90 * 20), 10),
			// 평균 80.0 + 포인트 35 = 115.0 → 평균은 낮아도 포인트로 1위
			withPoints(member(2L, NORTHWEST, 1, 20, 20, 80 * 20), 35));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, OPEN);

		assertThat(board.region("SEOUL_NORTHWEST").rankings())
			.extracting(Ranked::rank, Ranked::driverId, Ranked::averageScore, Ranked::points, Ranked::score)
			.containsExactly(tuple(1, 2L, 80.0, 35, 115.0), tuple(2, 1L, 90.0, 10, 100.0));
		assertThat(board.standingOf(1L)).hasValueSatisfying(standing -> {
			assertThat(standing.averageScore()).isEqualTo(90.0);
			assertThat(standing.points()).isEqualTo(10);
			assertThat(standing.score()).isEqualTo(100.0);
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

	@Test
	void 리그_점수가_있는_참여자가_10명_미만인_권역은_순위를_공개하지_않는다() {
		List<MemberRecord> members = new java.util.ArrayList<>();
		for (long id = 1; id <= 9; id++) {
			members.add(member(id, NORTHWEST, 10, 60, 60, 80 * 60));
		}
		members.add(member(10L, NORTHWEST, 1, 5, 0, 0));
		for (long id = 11; id <= 20; id++) {
			members.add(member(id, BUSAN, 10, 60, 60, 80 * 60));
		}

		LeagueBoard board = LeagueBoard.build(members, REGIONS, new LeagueBoard.Rules(10, 50, 10));

		LeagueBoard.RegionBoard northwest = board.region("SEOUL_NORTHWEST");
		assertThat(northwest.participants()).isEqualTo(9);
		assertThat(northwest.published()).isFalse();
		assertThat(northwest.rankings()).isEmpty();
		assertThat(board.standingOf(1L)).hasValueSatisfying(standing -> assertThat(standing.rank()).isNull());
		assertThat(board.region("BUSAN").published()).isTrue();
		assertThat(board.region("BUSAN").rankings()).hasSize(10);
	}

	@Test
	void 순위_공개를_끈_운전자는_참여자_수와_순위에서_빠진다() {
		List<MemberRecord> members = new java.util.ArrayList<>();
		for (long id = 1; id <= 10; id++) {
			members.add(member(id, NORTHWEST, 10, 60, 60, 80 * 60));
		}
		members.add(hidden(member(11L, NORTHWEST, 10, 60, 60, 100 * 60)));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, new LeagueBoard.Rules(10, 50, 10));

		assertThat(board.region("SEOUL_NORTHWEST").participants()).isEqualTo(10);
		assertThat(board.region("SEOUL_NORTHWEST").rankings()).extracting(Ranked::driverId).doesNotContain(11L);
		assertThat(board.standingOf(11L)).hasValueSatisfying(standing -> assertThat(standing.rank()).isNull());
	}

	@Test
	void 지역_안전지수는_권역_리그_점수의_거리_가중_평균이고_높은_순으로_권역_순위를_매긴다() {
		List<MemberRecord> members = List.of(
			// 서북권: (90 × 10 + 60 × 30) ÷ 40 = 67.5. 순위 자격이 없어도 안전지수에는 들어간다.
			member(1L, NORTHWEST, 1, 10, 10, 90 * 10),
			member(2L, NORTHWEST, 1, 30, 30, 60 * 30),
			// 순위 공개를 끈 운전자는 안전지수에서도 빠진다(D8).
			hidden(member(3L, NORTHWEST, 10, 100, 100, 100 * 100)),
			member(4L, BUSAN, 1, 20, 20, 80 * 20));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, new LeagueBoard.Rules(1, 50, 10));

		assertThat(board.regions())
			.extracting(r -> r.region().code(), LeagueBoard.RegionBoard::safetyIndex, LeagueBoard.RegionBoard::rank)
			.containsExactly(tuple("SEOUL_NORTHWEST", 67.5, 2), tuple("BUSAN", 80.0, 1), tuple("DAEGU", null, null));
	}

	@Test
	void 시즌_보상은_순위가_공개된_권역_중_안전지수_1위_권역의_자격자에게_간다() {
		List<MemberRecord> members = List.of(
			member(1L, NORTHWEST, 10, 60, 60, 90 * 60),
			member(2L, NORTHWEST, 1, 10, 10, 90 * 10),
			hidden(member(3L, NORTHWEST, 10, 60, 60, 90 * 60)),
			member(4L, NORTHWEST, 10, 60, 60, 90 * 60),
			// 부산은 안전지수가 더 높지만 참여자가 기준(2명) 미만이라 순위 비공개 → 1위 후보가 아니다
			member(5L, BUSAN, 10, 60, 60, 99 * 60),
			member(6L, DAEGU, 10, 60, 60, 80 * 60),
			member(7L, DAEGU, 10, 60, 60, 80 * 60));

		LeagueBoard board = LeagueBoard.build(members, REGIONS, new LeagueBoard.Rules(2, 50, 10));

		// 2번은 자격 미달(10km · 1회), 3번은 순위 공개를 껐다
		assertThat(board.seasonRewardees()).containsExactlyInAnyOrder(1L, 4L);
	}

	static MemberRecord hidden(MemberRecord member) {
		return new MemberRecord(member.driverId(), member.nickname(), member.leagueCode(), false, member.trips(),
			member.distanceKm(), member.scoredDistanceKm(), member.weightedScoreSum(), member.seasonPoints());
	}

	static MemberRecord member(Long driverId, LeagueRegion region, int trips, double distanceKm,
		double scoredDistanceKm, double weightedScoreSum) {
		return new MemberRecord(driverId, "driver" + driverId, region.code(), true, trips, distanceKm,
			scoredDistanceKm, weightedScoreSum, 0);
	}

	static MemberRecord withPoints(MemberRecord member, int seasonPoints) {
		return new MemberRecord(member.driverId(), member.nickname(), member.leagueCode(), member.visible(),
			member.trips(), member.distanceKm(), member.scoredDistanceKm(), member.weightedScoreSum(), seasonPoints);
	}
}
