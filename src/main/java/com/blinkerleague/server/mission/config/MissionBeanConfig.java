package com.blinkerleague.server.mission.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.mission.application.port.out.DriverMissionStore;
import com.blinkerleague.server.mission.application.port.out.LoadAchievedDaysPort;
import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;
import com.blinkerleague.server.mission.application.port.out.MissionRewardPort;
import com.blinkerleague.server.mission.application.port.out.SaveMissionPort;
import com.blinkerleague.server.mission.application.service.GetStreakService;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService;
import com.blinkerleague.server.mission.application.service.RecordMissionAfterTripService;
import com.blinkerleague.server.mission.application.service.SeedMissionService;
import com.blinkerleague.server.threshold.application.port.in.ThresholdProvider;

/** mission 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class MissionBeanConfig {

	@Bean
	public GetTodayMissionService getTodayMissionService(LoadActiveMissionPort loadActiveMissionPort,
		LoadDailyRoundsPort loadDailyRoundsPort, DriverMissionStore driverMissionStore, Clock clock) {
		return new GetTodayMissionService(loadActiveMissionPort, loadDailyRoundsPort, driverMissionStore, clock);
	}

	@Bean
	public SeedMissionService seedMissionService(SaveMissionPort saveMissionPort) {
		return new SeedMissionService(saveMissionPort);
	}

	@Bean
	public RecordMissionAfterTripService recordMissionAfterTripService(LoadActiveMissionPort loadActiveMissionPort,
		LoadDailyRoundsPort loadDailyRoundsPort, DriverMissionStore driverMissionStore,
		MissionRewardPort missionRewardPort, LoadAchievedDaysPort loadAchievedDaysPort,
		ThresholdProvider thresholdProvider, Clock clock) {
		return new RecordMissionAfterTripService(loadActiveMissionPort, loadDailyRoundsPort, driverMissionStore,
			missionRewardPort, loadAchievedDaysPort, thresholdProvider, clock);
	}

	@Bean
	public GetStreakService getStreakService(LoadAchievedDaysPort loadAchievedDaysPort, Clock clock) {
		return new GetStreakService(loadAchievedDaysPort, clock);
	}
}
