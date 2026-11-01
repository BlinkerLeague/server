package com.blinkerleague.server.driver.domain;

import lombok.Getter;

/** 운전자. 실명 · 연락처 같은 개인정보는 두지 않는다. */
@Getter
public class Driver {

	private Long id;
	private String nickname;
	private String regionCode;

	private Driver(String nickname, String regionCode) {
		this.nickname = nickname;
		this.regionCode = regionCode;
	}

	public static Driver register(String nickname, String regionCode) {
		return new Driver(nickname, regionCode);
	}
}
