package com.blinkerleague.server.driver.application.service;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.domain.Driver;

import lombok.RequiredArgsConstructor;

/** 리그 순위 공개를 켜고 끈다(D8, 설정 SCR-08). 끄면 리그(순위 · 안전지수 · 참여자 수 · 시즌 보상)에서 빠진다. */
@RequiredArgsConstructor
public class ChangeLeagueVisibilityService {

	private final LoadDriverPort loadDriverPort;
	private final SaveDriverPort saveDriverPort;

	public boolean change(Long driverId, boolean visible) {
		Driver driver = loadDriverPort.findById(driverId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "운전자를 찾을 수 없습니다."));
		driver.changeLeagueVisibility(visible);
		return saveDriverPort.save(driver).isLeagueVisible();
	}
}
