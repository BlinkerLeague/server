package com.blinkerleague.server.mission.application.service;

import java.util.List;

import com.blinkerleague.server.mission.application.port.out.SaveMissionPort;
import com.blinkerleague.server.mission.domain.Mission;

import lombok.RequiredArgsConstructor;

/** 시작할 때 기본 미션을 넣는다. 이미 미션이 있으면 건드리지 않는다. */
@RequiredArgsConstructor
public class SeedMissionService {

	private final SaveMissionPort saveMissionPort;

	public void seedIfAbsent(List<Mission> defaults) {
		throw new UnsupportedOperationException();
	}
}
