package com.blinkerleague.server.badge.domain;

/** 배지 판정에 쓰는 운전자의 누적 기록. */
public record BadgeProgress(int scoredTrips, int longestStreakDays, int signalOnTurns, int seasonChampions) {
}
