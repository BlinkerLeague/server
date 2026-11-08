package com.blinkerleague.server.mission.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.DailyPeriod;
import com.blinkerleague.server.mission.domain.DayJudgement;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionStatus;

import lombok.RequiredArgsConstructor;

/** 오늘의 미션 조회(plan.md 3.6, GET /api/missions/today). */
@RequiredArgsConstructor
public class GetTodayMissionService {

	private final LoadActiveMissionPort loadActiveMissionPort;
	private final LoadDailyRoundsPort loadDailyRoundsPort;
	private final DriverMissionStore driverMissionStore;
	private final Clock clock;

	public TodayMission today(Long driverId) {
		DailyPeriod today = DailyPeriod.of(clock.instant(), clock.getZone());
		Mission mission = loadActiveMissionPort.findActiveDaily(today.date())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "오늘 운영 중인 미션이 없습니다."));
		List<RoundTurns> rounds = loadDailyRoundsPort.load(driverId, today.start(), today.end());
		if (rounds.isEmpty()) {
			// 오늘 첫 운행 전에는 마지막 미션 날의 최종 상태(ACHIEVED · FAILED)를 계속 보여 준다.
			Optional<TodayMission> lastDay = driverMissionStore.findLatestBefore(driverId, today.key())
				.flatMap(record -> settledDay(driverId, LocalDate.parse(record.getPeriodKey())));
			if (lastDay.isPresent()) {
				return lastDay.get();
			}
		}
		return view(mission, today, rounds, false);
	}

	private Optional<TodayMission> settledDay(Long driverId, LocalDate date) {
		DailyPeriod day = DailyPeriod.of(date.atStartOfDay(clock.getZone()).toInstant(), clock.getZone());
		return loadActiveMissionPort.findActiveDaily(date)
			.map(mission -> view(mission, day, loadDailyRoundsPort.load(driverId, day.start(), day.end()), true));
	}

	private static TodayMission view(Mission mission, DailyPeriod period, List<RoundTurns> rounds, boolean dayEnded) {
		DayJudgement day = mission.getCondition().judgeDay(rounds);
		return new TodayMission(mission.getId(), mission.getTitle(), period.key(), day.status().settle(dayEnded),
			mission.getRewardPoint(), day.shownRound(), mission.getCondition().requiredTurns(), day.conditions());
	}

	/** shownRound는 진행 링에 보여 줄 라운드다(달성했으면 달성한 라운드, 아니면 가장 최근 라운드). */
	public record TodayMission(Long missionId, String title, String periodKey, MissionStatus status,
		int rewardPoint, RoundTurns shownRound, int requiredTurns, List<ConditionProgress> conditions) {
	}
}
