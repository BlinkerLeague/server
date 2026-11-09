package com.blinkerleague.server.league.domain;

/**
 * 순위 목록의 한 줄. score = averageScore(리그 점수 거리 가중 평균) + points(시즌 포인트)이고(D34),
 * 같은 점수는 같은 순위다(1, 2, 2, 4).
 */
public record Ranked(int rank, Long driverId, String nickname, double averageScore, int points, double score) {
}
