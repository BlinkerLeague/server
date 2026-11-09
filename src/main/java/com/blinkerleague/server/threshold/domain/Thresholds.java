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
	SignalCancel signalCancel,
	SeatBelt seatBelt,
	Rain rain,
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

	/** 운행 참여 최소 거리(D10), 앱의 주차 판정 시간, 한 운행의 이벤트 상한(D16), 서버 시각보다 미래로 허용하는 종료 시각(분, D17). */
	public record Trip(double minDistanceKm, int parkEndMinutes, int maxEvents, int maxFutureMinutes) {
	}

	/**
	 * 회전 판정 기준. 앱이 쓰는 값: 핸들 각도로 회전 감지(steeringExitDeg를 넘은 때부터 다시 내려올 때까지 한 번의 조향,
	 * 그 사이 steeringEnterDeg 이상이면 회전, mergeGapSec 안에 이어진 같은 방향 조향은 하나로 합침, 앱 요청 1번),
	 * 조향각 부호(steeringRightPositive: false면 음수가 오른쪽, 실차가 반대면 이 값만 바꾼다, 앱 요청 7번),
	 * 제때 깜빡이 거리(signalLeadDistanceM). windowSec · minAngleDeg · uTurnAngleDeg는 진행 방향 감지용이었고 앱은 쓰지 않는다.
	 * 서버가 쓰는 값: 늦은 깜빡이 점수(lateSignalScore, 제때 1.0 대비), 회전 속도 기준(turnSpeedKmh).
	 */
	public record Turn(int minSpeedKmh, int windowSec, int minAngleDeg, int uTurnAngleDeg, int signalLeadSec,
		double lateSignalScore, int turnSpeedKmh, int signalLeadDistanceM, int steeringEnterDeg, int steeringExitDeg,
		int mergeGapSec, boolean steeringRightPositive) {
	}

	/** 앱의 급가감속 판정 기준(deltaKmhPerSec)과 서버의 감점 폭(10km당 1회마다 penaltyPer10Km점). */
	public record Harsh(int deltaKmhPerSec, double penaltyPer10Km) {
	}

	/** 같은 경고를 1회로 묶는 시간(mergeWindowSec)과 감점 폭(100km당 1회마다 penaltyPer100Km점). */
	public record SafetyWarning(int mergeWindowSec, double penaltyPer100Km) {
	}

	/** 운행 적립 최대 거리, 일일 상한, 연속 달성 보상(streakRewardDays일마다 streakRewardPoint, plan.md 4.3). */
	/** 앱의 깜빡이 끄기 판정(forgetSec 넘게, minSpeedKmh 이상)과 서버의 1회당 감점(penalty). */
	public record SignalCancel(int forgetSec, int minSpeedKmh, int penalty) {
	}

	/** 안전벨트 감점: 출발 시 미착용(departurePenalty), 주행 중 미착용 1분마다(perMinutePenalty). */
	public record SeatBelt(int departurePenalty, int perMinutePenalty) {
	}

	/** 앱의 우천 판정: 와이퍼가 minWiperSec 넘게 켜진 구간, 제한속도의 speedRatio 이하를 감속 준수로 본다. */
	public record Rain(int minWiperSec, double speedRatio) {
	}

	public record Point(int tripMaxKm, int dailyCap, int streakRewardDays, int streakRewardPoint) {
	}

	public record GradeRule(String grade, int minScore) {
	}
}
