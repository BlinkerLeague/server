package com.blinkerleague.server.driver.application.service;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort.DriverTokens;
import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.domain.Driver;

import lombok.RequiredArgsConstructor;

/** 운전자 등록. 로그인이 없으므로 등록 시점에 토큰을 처음 발급한다(토큰 분실 시 재등록, plan.md 3.2). */
@RequiredArgsConstructor
public class RegisterDriverService {

	private final LoadDriverPort loadDriverPort;
	private final SaveDriverPort saveDriverPort;
	private final IssueDriverTokensPort issueDriverTokensPort;

	public Result register(Command command) {
		if (loadDriverPort.existsByNickname(command.nickname())) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다.");
		}
		Driver driver = saveDriverPort.save(Driver.register(command.nickname(), command.regionCode()));
		DriverTokens tokens = issueDriverTokensPort.issue(driver.getId());
		return new Result(driver.getId(), driver.getNickname(), driver.getRegionCode(), tokens.accessToken(),
			tokens.refreshToken());
	}

	public record Command(String nickname, String regionCode) {
	}

	public record Result(Long driverId, String nickname, String regionCode, String accessToken, String refreshToken) {
	}
}
