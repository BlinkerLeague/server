package com.blinkerleague.server.league.domain;

/** 순위 목록의 한 줄. 같은 점수는 같은 순위다(1, 2, 2, 4). */
public record Ranked(int rank, Long driverId, String nickname, double score) {

	public double averageScore() {
		throw new UnsupportedOperationException();
	}

	public int points() {
		throw new UnsupportedOperationException();
	}
}
