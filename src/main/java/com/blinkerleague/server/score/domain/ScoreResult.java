package com.blinkerleague.server.score.domain;

/** 운행 한 건의 점수 계산 결과. 측정할 항목이 없으면 score는 null이다. */
public record ScoreResult(Integer score) {
}
