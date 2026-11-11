package com.blinkerleague.server.threshold.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 기준값을 저장하기 전에 확인하는 규칙. 위반 문구 목록을 돌려주고, 비어 있으면 저장해도 된다. */
public final class ThresholdsValidator {

	private ThresholdsValidator() {
	}

	public static List<String> validate(Thresholds thresholds) {
		List<String> violations = missingGroups(thresholds);
		if (!violations.isEmpty()) {
			return violations;
		}
		checkWeights(violations, thresholds);
		checkGrades(violations, thresholds.grades());
		checkRanges(violations, thresholds);
		if (thresholds.league().commonItems().isEmpty()) {
			violations.add("league.commonItems는 하나 이상이어야 합니다.");
		}
		return violations;
	}

	/**
	 * 점수는 측정한 항목끼리 합이 1이 되도록 다시 나누므로 합 자체는 1이 아니어도 된다. 다만 측정한 항목의 가중치 합으로
	 * 나누므로 각 항목은 0보다 커야 한다(0인 항목만 측정되면 0으로 나누게 된다).
	 */
	private static void checkWeights(List<String> violations, Thresholds thresholds) {
		for (ScoreItem item : ScoreItem.values()) {
			Double weight = thresholds.weights().get(item);
			if (weight == null) {
				violations.add("weights." + item + " 값이 없습니다.");
			} else if (weight <= 0) {
				violations.add("weights." + item + " 값은 0보다 커야 합니다.");
			}
		}
		thresholds.minSamples().forEach((item, minSamples) -> {
			if (minSamples < 1) {
				violations.add("minSamples." + item + " 값은 1 이상이어야 합니다.");
			}
		});
	}

	private static void checkGrades(List<String> violations, List<Thresholds.GradeRule> grades) {
		grades.forEach(rule -> {
			if (rule.minScore() < 0 || rule.minScore() > 100) {
				violations.add("grades." + rule.grade() + " 기준 점수는 0~100 사이여야 합니다.");
			}
		});
		duplicates(grades.stream().map(Thresholds.GradeRule::grade).toList())
			.forEach(grade -> violations.add("grades 등급 이름이 겹칩니다: " + grade));
		duplicates(grades.stream().map(Thresholds.GradeRule::minScore).toList())
			.forEach(minScore -> violations.add("grades 기준 점수가 겹칩니다: " + minScore));
	}

	/** 음수 감점은 점수를 올리고, 0인 연속 보상 주기는 나눗셈을 깨므로 범위를 확인한다. */
	private static void checkRanges(List<String> violations, Thresholds thresholds) {
		nonNegative(violations, "trip.minDistanceKm", thresholds.trip().minDistanceKm());
		nonNegative(violations, "trip.maxFutureMinutes", thresholds.trip().maxFutureMinutes());
		nonNegative(violations, "harsh.penaltyPer10Km", thresholds.harsh().penaltyPer10Km());
		nonNegative(violations, "safetyWarning.penaltyPer100Km", thresholds.safetyWarning().penaltyPer100Km());
		nonNegative(violations, "signalCancel.penalty", thresholds.signalCancel().penalty());
		nonNegative(violations, "seatBelt.departurePenalty", thresholds.seatBelt().departurePenalty());
		nonNegative(violations, "seatBelt.perMinutePenalty", thresholds.seatBelt().perMinutePenalty());
		nonNegative(violations, "point.tripMaxKm", thresholds.point().tripMaxKm());
		nonNegative(violations, "point.dailyCap", thresholds.point().dailyCap());
		nonNegative(violations, "point.streakRewardPoint", thresholds.point().streakRewardPoint());
		nonNegative(violations, "league.minSeasonDistanceKm", thresholds.league().minSeasonDistanceKm());
		nonNegative(violations, "league.minSeasonTrips", thresholds.league().minSeasonTrips());
		atLeastOne(violations, "trip.maxEvents", thresholds.trip().maxEvents());
		atLeastOne(violations, "trip.maxPastDays", thresholds.trip().maxPastDays());
		atLeastOne(violations, "point.streakRewardDays", thresholds.point().streakRewardDays());
		atLeastOne(violations, "league.minParticipants", thresholds.league().minParticipants());
		atLeastOne(violations, "league.topN", thresholds.league().topN());
		ratio(violations, "turn.lateSignalScore", thresholds.turn().lateSignalScore());
		ratio(violations, "rain.speedRatio", thresholds.rain().speedRatio());
	}

	/** 그룹이 하나라도 빠지면 나머지 규칙은 볼 수 없으므로 빠진 그룹만 돌려준다. */
	private static List<String> missingGroups(Thresholds thresholds) {
		Map<String, Object> groups = new LinkedHashMap<>();
		groups.put("trip", thresholds.trip());
		groups.put("turn", thresholds.turn());
		groups.put("harsh", thresholds.harsh());
		groups.put("safetyWarning", thresholds.safetyWarning());
		groups.put("signalCancel", thresholds.signalCancel());
		groups.put("seatBelt", thresholds.seatBelt());
		groups.put("rain", thresholds.rain());
		groups.put("weights", thresholds.weights());
		groups.put("minSamples", thresholds.minSamples());
		groups.put("point", thresholds.point());
		groups.put("league", thresholds.league());
		groups.put("grades", thresholds.grades());
		groups.put("tips", thresholds.tips());
		List<String> missing = new ArrayList<>();
		groups.forEach((name, value) -> {
			if (value == null) {
				missing.add(name + " 값이 없습니다.");
			}
		});
		return missing;
	}

	private static void nonNegative(List<String> violations, String path, double value) {
		if (value < 0) {
			violations.add(path + " 값은 0 이상이어야 합니다.");
		}
	}

	private static void atLeastOne(List<String> violations, String path, int value) {
		if (value < 1) {
			violations.add(path + " 값은 1 이상이어야 합니다.");
		}
	}

	private static void ratio(List<String> violations, String path, double value) {
		if (value < 0 || value > 1) {
			violations.add(path + " 값은 0~1 사이여야 합니다.");
		}
	}

	private static <T> Set<T> duplicates(List<T> values) {
		Set<T> seen = new HashSet<>();
		Set<T> duplicated = new LinkedHashSet<>();
		values.forEach(value -> {
			if (!seen.add(value)) {
				duplicated.add(value);
			}
		});
		return duplicated;
	}
}
