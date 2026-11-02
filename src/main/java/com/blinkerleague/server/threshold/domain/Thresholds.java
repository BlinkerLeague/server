package com.blinkerleague.server.threshold.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 판정 기준값 · 점수 가중치 · 포인트 규칙 · 등급 기준 · 팁 문구(plan.md 3.3).
 * 앱과 서버가 같은 값을 쓰며, 코드에 상수로 두지 않는다(plan.md 6장).
 */
public record Thresholds(
	Trip trip,
	Turn turn,
	Harsh harsh,
	SafetyWarning safetyWarning,
	Map<ScoreItem, Double> weights,
	Point point,
	List<GradeRule> grades,
	Map<ScoreItem, String> tips) {

	/** 점수에 해당하는 등급. */
	public Optional<String> gradeOf(int score) {
		throw new UnsupportedOperationException();
	}

	public record Trip(double minDistanceKm, int parkEndMinutes) {
	}

	public record Turn(int minSpeedKmh, int windowSec, int minAngleDeg, int uTurnAngleDeg, int signalLeadSec) {
	}

	public record Harsh(int deltaKmhPerSec) {
	}

	public record SafetyWarning(int mergeWindowSec) {
	}

	public record Point(int tripMaxKm, int dailyCap) {
	}

	public record GradeRule(String grade, int minScore) {
	}
}
