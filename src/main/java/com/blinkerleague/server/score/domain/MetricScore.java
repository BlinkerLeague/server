package com.blinkerleague.server.score.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 항목 하나의 측정값(0~1), 점수(0~100), 재분배 후 가중치. */
public record MetricScore(ScoreItem metric, double value, int score, double weight) {
}
