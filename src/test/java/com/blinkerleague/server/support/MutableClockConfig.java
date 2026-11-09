package com.blinkerleague.server.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** {@code @Import(MutableClockConfig.class)}로 서버 시계를 {@link MutableClock}으로 바꾼다. */
@TestConfiguration
public class MutableClockConfig {

	@Bean
	@Primary
	public MutableClock mutableClock() {
		return new MutableClock();
	}
}
