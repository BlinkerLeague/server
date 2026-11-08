package com.blinkerleague.server.mission.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.mission.application.port.out.LoadDailyTurnsPort;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService.TodayMission;
import com.blinkerleague.server.mission.domain.ConditionProgress;
import com.blinkerleague.server.mission.domain.ConditionProgress.ConditionType;
import com.blinkerleague.server.mission.domain.RoundTurns;
import com.blinkerleague.server.mission.domain.Mission;
import com.blinkerleague.server.mission.domain.MissionStatus;
import com.blinkerleague.server.mission.domain.TurnSignalCondition;

class GetTodayMissionServiceTest {

	// 2026-11-03 12:00 KST
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T03:00:00Z"), ZoneId.of("Asia/Seoul"));

	private final Mission seed = Mission.daily("회전 깜빡이 완벽", new TurnSignalCondition(3, 100), 20,
		LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31));

	private final RecordingTurns turns = new RecordingTurns(new RoundTurns(2, 2));

	private final GetTodayMissionService service = new GetTodayMissionService(
		date -> Optional.of(seed), turns, clock);

	@Test
	void 한국_날짜로_오늘_운행의_회전을_모아_미션_진행도를_돌려준다() {
		TodayMission today = service.today(1L);

		assertThat(today.title()).isEqualTo("회전 깜빡이 완벽");
		assertThat(today.periodKey()).isEqualTo("2026-11-03");
		assertThat(today.status()).isEqualTo(MissionStatus.IN_PROGRESS);
		assertThat(today.rewardPoint()).isEqualTo(20);
		assertThat(today.turns()).isEqualTo(new RoundTurns(2, 2));
		assertThat(today.requiredTurns()).isEqualTo(3);
		assertThat(today.conditions()).containsExactly(
			new ConditionProgress(ConditionType.TURN_COUNT, 2, 3),
			new ConditionProgress(ConditionType.SIGNAL_RATE, 100, 100));
		assertThat(turns.driverId).isEqualTo(1L);
		assertThat(turns.from).isEqualTo(Instant.parse("2026-11-02T15:00:00Z"));
		assertThat(turns.to).isEqualTo(Instant.parse("2026-11-03T15:00:00Z"));
	}

	@Test
	void 오늘_운영_중인_미션이_없으면_NOT_FOUND다() {
		GetTodayMissionService noMission = new GetTodayMissionService(date -> Optional.empty(), turns, clock);

		assertThatThrownBy(() -> noMission.today(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	/** 요청 받은 운전자와 기간을 기록하는 가짜 포트. */
	private static final class RecordingTurns implements LoadDailyTurnsPort {

		private final RoundTurns result;
		private Long driverId;
		private Instant from;
		private Instant to;

		RecordingTurns(RoundTurns result) {
			this.result = result;
		}

		@Override
		public RoundTurns load(Long driverId, Instant from, Instant to) {
			this.driverId = driverId;
			this.from = from;
			this.to = to;
			return result;
		}
	}
}
