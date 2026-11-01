package com.blinkerleague.server.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeConfigTest {

	@Test
	void Clock은_한국_시간대를_쓴다() {
		assertThat(new TimeConfig().clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
		assertThat(TimeConfig.ZONE).isEqualTo(ZoneId.of("Asia/Seoul"));
	}
}
