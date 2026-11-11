package com.blinkerleague.server.threshold.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 기준값을 저장하기 전에 확인하는 규칙. 위반 문구 목록을 돌려주고, 비어 있으면 저장해도 된다. */
public final class ThresholdsValidator {

	private ThresholdsValidator() {
	}

	public static List<String> validate(Thresholds thresholds) {
		List<String> violations = new ArrayList<>();
		thresholds.weights().forEach((item, weight) -> {
			if (weight < 0) {
				violations.add("weights." + item + " 값은 0 이상이어야 합니다.");
			}
		});
		if (thresholds.weights().values().stream().mapToDouble(Double::doubleValue).sum() <= 0) {
			violations.add("weights 합은 0보다 커야 합니다.");
		}
		thresholds.grades().forEach(rule -> {
			if (rule.minScore() < 0 || rule.minScore() > 100) {
				violations.add("grades." + rule.grade() + " 기준 점수는 0~100 사이여야 합니다.");
			}
		});
		duplicates(thresholds.grades().stream().map(Thresholds.GradeRule::grade).toList())
			.forEach(grade -> violations.add("grades 등급 이름이 겹칩니다: " + grade));
		duplicates(thresholds.grades().stream().map(Thresholds.GradeRule::minScore).toList())
			.forEach(minScore -> violations.add("grades 기준 점수가 겹칩니다: " + minScore));
		return violations;
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
