package com.blinkerleague.server.report.domain;

import java.util.List;

/** 한 주의 운행 기록: 점수 대상 운행 수 · 거리(최소 거리 미만 제외)와 측정한 항목 점수. */
public record WeekDriving(int trips, double distanceKm, List<TripMetricRecord> metrics) {
}
