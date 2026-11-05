package com.blinkerleague.server.score.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 운행 점수 엔진. DB · 스프링에 의존하지 않는 순수 계산이다(plan.md 3.5). */
public class ScoreCalculator {

	public ScoreResult calculate(Set<ScoreItem> supported, List<TurnRecord> turns, Map<ScoreItem, Double> weights) {
		if (turns.isEmpty()) {
			return new ScoreResult(null);
		}
		long signalOn = turns.stream().filter(TurnRecord::signalOn).count();
		return new ScoreResult((int) Math.round(signalOn * 100.0 / turns.size()));
	}
}
