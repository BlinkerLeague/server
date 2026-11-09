package com.blinkerleague.server.league.domain;

import java.util.List;
import java.util.Optional;

/**
 * 한 시즌의 리그 현황(plan.md 4.4). 권역별 순위 · 지역 안전지수와 권역 순위를 계산한다.
 * 저장소나 시계에 기대지 않는 순수 계산이다.
 */
public final class LeagueBoard {

	public static LeagueBoard build(List<MemberRecord> members, List<LeagueRegion> regions, Rules rules) {
		throw new UnsupportedOperationException();
	}

	public RegionBoard region(String leagueCode) {
		throw new UnsupportedOperationException();
	}

	public Optional<Standing> standingOf(Long driverId) {
		throw new UnsupportedOperationException();
	}

	/** 순위 공개 최소 참여자(D11), 순위 자격: 시즌 누적 거리 또는 운행 수(D13). */
	public record Rules(int minParticipants, double minSeasonDistanceKm, int minSeasonTrips) {
	}

	public record RegionBoard(LeagueRegion region, int participants, boolean published, Double safetyIndex,
		List<Ranked> rankings) {
	}

	/** 운전자의 시즌 점수와 자격. rank는 순위가 공개되고 자격이 있을 때만 있다. */
	public record Standing(String leagueCode, Double score, boolean eligible, Integer rank, int trips,
		double distanceKm) {
	}
}
