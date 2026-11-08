package com.blinkerleague.server.report.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 항목 하나의 이번 주 · 지난주 평균 점수와 변화. 그 주에 측정하지 않았으면 null이다. */
public record MetricChange(ScoreItem metric, Integer thisWeek, Integer lastWeek, Integer change) {
}
