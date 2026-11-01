package com.blinkerleague.server.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import com.blinkerleague.server.common.time.TimeConfig;

class JpaAuditingConfigTest {

	@Test
	void 감사_시각은_주입된_Clock의_한국_시간으로_채운다() {
		// UTC 15:30은 한국 시간으로 다음 날 00:30이다.
		Clock clock = Clock.fixed(Instant.parse("2026-11-01T15:30:00Z"), TimeConfig.ZONE);

		DateTimeProvider provider = new JpaAuditingConfig().auditingDateTimeProvider(clock);

		assertThat(provider.getNow()).contains(LocalDateTime.of(2026, 11, 2, 0, 30));
	}
}
