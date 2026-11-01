package com.blinkerleague.server.common.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 일 단위 집계(미션 period_key, 일일 포인트 상한 등)는 모두 한국 시간 기준이다.
 * 현재 시각이 필요한 곳은 {@link Clock}을 주입받아 테스트에서 시각을 고정할 수 있게 한다.
 */
@Configuration
public class TimeConfig {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(ZONE);
	}
}
