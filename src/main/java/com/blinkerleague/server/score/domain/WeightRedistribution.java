package com.blinkerleague.server.score.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 측정 불가 항목을 빼고 남은 가중치를 합이 1이 되도록 비율대로 나눈다(plan.md 3.5). */
public final class WeightRedistribution {

	private WeightRedistribution() {
	}

	public static Map<ScoreItem, Double> redistribute(Map<ScoreItem, Double> weights, Set<ScoreItem> measured) {
		double total = measured.stream().mapToDouble(item -> weights.getOrDefault(item, 0.0)).sum();
		Map<ScoreItem, Double> result = new EnumMap<>(ScoreItem.class);
		measured.forEach(item -> result.put(item, weights.getOrDefault(item, 0.0) / total));
		return result;
	}
}
