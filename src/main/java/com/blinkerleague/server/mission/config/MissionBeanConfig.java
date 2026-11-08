package com.blinkerleague.server.mission.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.blinkerleague.server.mission.application.port.out.LoadActiveMissionPort;
import com.blinkerleague.server.mission.application.port.out.LoadDailyRoundsPort;
import com.blinkerleague.server.mission.application.port.out.SaveMissionPort;
import com.blinkerleague.server.mission.application.service.GetTodayMissionService;
import com.blinkerleague.server.mission.application.service.SeedMissionService;

/** mission 유스케이스에 포트 구현(어댑터)을 연결한다. */
@Configuration
public class MissionBeanConfig {

	@Bean
	public GetTodayMissionService getTodayMissionService(LoadActiveMissionPort loadActiveMissionPort,
		LoadDailyRoundsPort loadDailyRoundsPort, Clock clock) {
		return new GetTodayMissionService(loadActiveMissionPort, loadDailyRoundsPort, clock);
	}

	@Bean
	public SeedMissionService seedMissionService(SaveMissionPort saveMissionPort) {
		return new SeedMissionService(saveMissionPort);
	}
}
