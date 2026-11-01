package com.blinkerleague.server.driver.application.service;

import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;

import lombok.RequiredArgsConstructor;

/** 운전자 등록. 로그인이 없으므로 등록 시점에 토큰을 처음 발급한다(토큰 분실 시 재등록, plan.md 3.2). */
@RequiredArgsConstructor
public class RegisterDriverService {

	private final SaveDriverPort saveDriverPort;
	private final IssueDriverTokensPort issueDriverTokensPort;

	public Result register(Command command) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	public record Command(String nickname, String regionCode) {
	}

	public record Result(Long driverId, String nickname, String regionCode, String accessToken, String refreshToken) {
	}
}
