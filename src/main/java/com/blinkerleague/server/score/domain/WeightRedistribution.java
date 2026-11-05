package com.blinkerleague.server.score.domain;

import java.util.Map;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 측정 불가 항목을 빼고 남은 가중치를 합이 1이 되도록 비율대로 나눈다(plan.md 3.5). */
public final class WeightRedistribution {

	private WeightRedistribution() {
	}

	public static Map<ScoreItem, Double> redistribute(Map<ScoreItem, Double> weights, Set<ScoreItem> measured) {
		throw new UnsupportedOperationException();
	}
}
