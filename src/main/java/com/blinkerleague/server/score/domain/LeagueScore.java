package com.blinkerleague.server.score.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/**
 * 리그 비교 점수(D19). 차마다 측정 항목이 달라도 같은 기준으로 겨루도록, 모든 차가 측정하는 공통 항목만으로
 * 가중치를 다시 나눠 매긴다. 운행 점수 · 포인트는 계속 전체 항목 점수를 쓴다.
 */
public final class LeagueScore {

	private LeagueScore() {
	}

	public static Integer of(List<MetricScore> metrics, Map<ScoreItem, Double> weights, Set<ScoreItem> commonItems) {
		throw new UnsupportedOperationException();
	}
}
