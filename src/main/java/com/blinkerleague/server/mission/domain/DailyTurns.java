package com.blinkerleague.server.mission.domain;

/** 그날(Asia/Seoul) 점수 대상 운행들의 회전 수와 깜빡이를 켠 회전 수. */
public record DailyTurns(int turns, int successTurns) {
}
