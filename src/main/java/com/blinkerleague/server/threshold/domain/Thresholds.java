package com.blinkerleague.server.threshold.domain;

import java.util.Comparator;
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
	Map<ScoreItem, Integer> minSamples,
	Point point,
	List<GradeRule> grades,
	Map<ScoreItem, String> tips) {

	/** 높은 기준부터 비교해 처음 만족하는 등급. 최저 기준 미만이면 등급이 없다(표기는 화면 기획서와 확정). */
	public Optional<String> gradeOf(int score) {
		return grades.stream()
			.sorted(Comparator.comparingInt(GradeRule::minScore).reversed())
			.filter(rule -> score >= rule.minScore())
			.map(GradeRule::grade)
			.findFirst();
	}

	public record Trip(double minDistanceKm, int parkEndMinutes) {
	}

	/** 앱의 회전 판정 기준과 서버의 늦은 깜빡이 점수(lateSignalScore, 제때 1.0 대비). */
	public record Turn(int minSpeedKmh, int windowSec, int minAngleDeg, int uTurnAngleDeg, int signalLeadSec,
		double lateSignalScore) {
	}

	/** 앱의 급가감속 판정 기준(deltaKmhPerSec)과 서버의 감점 폭(10km당 1회마다 penaltyPer10Km점). */
	public record Harsh(int deltaKmhPerSec, double penaltyPer10Km) {
	}

	/** 같은 경고를 1회로 묶는 시간(mergeWindowSec)과 감점 폭(100km당 1회마다 penaltyPer100Km점). */
	public record SafetyWarning(int mergeWindowSec, double penaltyPer100Km) {
	}

	/** 운행 적립 최대 거리, 일일 상한, 연속 달성 보상(streakRewardDays일마다 streakRewardPoint, plan.md 4.3). */
	public record Point(int tripMaxKm, int dailyCap, int streakRewardDays, int streakRewardPoint) {
	}

	public record GradeRule(String grade, int minScore) {
	}
}
