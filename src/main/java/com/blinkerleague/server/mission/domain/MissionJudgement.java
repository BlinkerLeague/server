package com.blinkerleague.server.mission.domain;

import java.util.List;

/** 미션 조건을 라운드 1건의 회전 결과에 적용한 판정. */
public record MissionJudgement(MissionStatus status, List<ConditionProgress> conditions) {
}
