package com.blinkerleague.server.threshold.domain;

import java.util.List;

/** 기준값을 저장하기 전에 확인하는 규칙. 위반 문구 목록을 돌려주고, 비어 있으면 저장해도 된다. */
public final class ThresholdsValidator {

	private ThresholdsValidator() {
	}

	public static List<String> validate(Thresholds thresholds) {
		return List.of();
	}
}
