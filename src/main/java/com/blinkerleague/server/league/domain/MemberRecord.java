package com.blinkerleague.server.league.domain;

/**
 * 시즌 한 운전자의 리그 기록. 소속 권역은 시즌에 처음 운행할 때 고정한다(D12).
 * trips · distanceKm은 참여 운행(최소 거리 이상) 합계로 순위 자격(D13)에 쓰고,
 * scoredDistanceKm · weightedScoreSum은 리그 점수(D19)가 있는 운행의 거리 합과 (리그 점수 × 거리) 합이다.
 */
public record MemberRecord(Long driverId, String nickname, String leagueCode, boolean visible, int trips,
	double distanceKm, double scoredDistanceKm, double weightedScoreSum) {

	/** 리그 점수가 있는 운행이 하나라도 있으면 리그 점수를 겨룬다. */
	public boolean hasScore() {
		return scoredDistanceKm > 0;
	}
}
