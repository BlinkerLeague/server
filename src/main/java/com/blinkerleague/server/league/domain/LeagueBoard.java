package com.blinkerleague.server.league.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한 시즌의 리그 현황(plan.md 4.4). 권역별 순위 · 지역 안전지수와 권역 순위를 계산한다.
 * 저장소나 시계에 기대지 않는 순수 계산이다.
 */
public final class LeagueBoard {

	private final Map<String, RegionBoard> regions;
	private final Map<Long, Standing> standings;

	private LeagueBoard(Map<String, RegionBoard> regions, Map<Long, Standing> standings) {
		this.regions = regions;
		this.standings = standings;
	}

	public static LeagueBoard build(List<MemberRecord> members, List<LeagueRegion> regions, Rules rules) {
		Map<String, RegionBoard> boards = new LinkedHashMap<>();
		Map<Long, Standing> standings = new HashMap<>();
		for (LeagueRegion region : regions) {
			List<MemberRecord> inRegion = members.stream()
				.filter(member -> member.leagueCode().equals(region.code()))
				.toList();
			List<Ranked> rankings = rank(inRegion.stream()
				.filter(member -> member.hasScore() && rules.eligible(member))
				.toList());
			boards.put(region.code(), new RegionBoard(region, inRegion.size(), true, null, rankings));
			Map<Long, Integer> ranks = new HashMap<>();
			rankings.forEach(ranked -> ranks.put(ranked.driverId(), ranked.rank()));
			inRegion.forEach(member -> standings.put(member.driverId(), new Standing(region.code(),
				member.hasScore() ? scoreOf(member) : null, rules.eligible(member), ranks.get(member.driverId()),
				member.trips(),
				member.distanceKm())));
		}
		return new LeagueBoard(boards, standings);
	}

	public RegionBoard region(String leagueCode) {
		return regions.get(leagueCode);
	}

	public Optional<Standing> standingOf(Long driverId) {
		return Optional.ofNullable(standings.get(driverId));
	}

	// 점수 내림차순. 같은 점수는 같은 순위이고 다음 순위는 그 수만큼 건너뛴다(1, 2, 2, 4).
	private static List<Ranked> rank(List<MemberRecord> members) {
		List<MemberRecord> sorted = members.stream()
			.sorted(Comparator.comparingDouble(LeagueBoard::scoreOf).reversed()
				.thenComparing(MemberRecord::driverId))
			.toList();
		List<Ranked> rankings = new ArrayList<>();
		for (int i = 0; i < sorted.size(); i++) {
			MemberRecord member = sorted.get(i);
			double score = scoreOf(member);
			int rank = i > 0 && rankings.get(i - 1).score() == score ? rankings.get(i - 1).rank() : i + 1;
			rankings.add(new Ranked(rank, member.driverId(), member.nickname(), score));
		}
		return rankings;
	}

	// 리그 점수의 거리 가중 평균, 소수 첫째 자리
	private static double scoreOf(MemberRecord member) {
		return round1(member.weightedScoreSum() / member.scoredDistanceKm());
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}

	/** 순위 공개 최소 참여자(D11), 순위 자격: 시즌 누적 거리 또는 운행 수(D13). */
	public record Rules(int minParticipants, double minSeasonDistanceKm, int minSeasonTrips) {

		boolean eligible(MemberRecord member) {
			return member.distanceKm() >= minSeasonDistanceKm || member.trips() >= minSeasonTrips;
		}
	}

	public record RegionBoard(LeagueRegion region, int participants, boolean published, Double safetyIndex,
		List<Ranked> rankings) {
	}

	/** 운전자의 시즌 점수와 자격. rank는 순위가 공개되고 자격이 있을 때만 있다. */
	public record Standing(String leagueCode, Double score, boolean eligible, Integer rank, int trips,
		double distanceKm) {
	}
}
