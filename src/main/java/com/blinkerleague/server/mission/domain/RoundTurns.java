package com.blinkerleague.server.mission.domain;

/** 라운드(시동 켜고 끄기까지 운행 1건)의 회전 수와 깜빡이를 켠 회전 수. */
public record RoundTurns(int turns, int successTurns) {
}
