package com.blinkerleague.server.mission.domain;

import java.util.List;

/**
 * 하루 미션 판정. 그날 라운드 중 하나라도 조건을 채우면 ACHIEVED로 확정한다.
 * shownRound는 진행 링에 보여 줄 라운드(달성했으면 달성한 라운드, 아니면 가장 최근 라운드)다.
 */
public record DayJudgement(MissionStatus status, RoundTurns shownRound, List<ConditionProgress> conditions) {
}
