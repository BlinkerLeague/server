package com.blinkerleague.server.report.domain;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 주간 집계에 쓰는 운행 한 건의 항목 점수(측정한 항목만)와 운행 거리. */
public record TripMetricRecord(String tripId, ScoreItem metric, int score, double distanceKm) {
}
