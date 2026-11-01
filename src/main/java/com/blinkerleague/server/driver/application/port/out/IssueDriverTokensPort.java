package com.blinkerleague.server.driver.application.port.out;

/** 운전자에게 Access · Refresh 토큰을 처음 발급한다. 운영 구현은 auth 기능을 쓰는 어댑터다. */
public interface IssueDriverTokensPort {

	DriverTokens issue(Long driverId);

	record DriverTokens(String accessToken, String refreshToken) {
	}
}
