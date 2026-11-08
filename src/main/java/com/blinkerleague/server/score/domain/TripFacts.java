package com.blinkerleague.server.score.domain;

import java.util.List;
import java.util.Set;

import com.blinkerleague.server.threshold.domain.ScoreItem;

/** 점수 계산 입력: 차량이 측정할 수 있는 항목, 운행 거리, 운행 이벤트. */
public record TripFacts(Set<ScoreItem> supported, double distanceKm, List<DrivingEvent> events) {
}
