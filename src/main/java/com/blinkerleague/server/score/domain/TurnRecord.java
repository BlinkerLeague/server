package com.blinkerleague.server.score.domain;

/** 점수 계산에 쓰는 회전 한 번. 깜빡이를 켰는지는 앱이 판정해 보낸다(plan.md 3.5). */
public record TurnRecord(boolean signalOn) {
}
