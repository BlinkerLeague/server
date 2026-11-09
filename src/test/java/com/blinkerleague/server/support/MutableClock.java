package com.blinkerleague.server.support;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.blinkerleague.server.common.time.TimeConfig;

/**
 * 테스트에서 서버의 "지금"을 고정하고 필요하면 옮기는 시계. 운행 시각 허용 범위(D17)처럼 현재 시각에 따라
 * 결과가 달라지는 규칙이 실제 날짜에 좌우되지 않게 한다.
 */
public class MutableClock extends Clock {

	/** 업로드 테스트들이 쓰는 운행 날짜(2026-11-03)의 저녁. 그날 운행은 모두 허용 범위 안이다. */
	public static final Instant DEFAULT = OffsetDateTime.parse("2026-11-03T21:00:00+09:00").toInstant();

	private Instant now = DEFAULT;

	public void setNow(Instant now) {
		this.now = now;
	}

	public void setNow(String offsetDateTime) {
		setNow(OffsetDateTime.parse(offsetDateTime).toInstant());
	}

	public void reset() {
		now = DEFAULT;
	}

	@Override
	public ZoneId getZone() {
		return TimeConfig.ZONE;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return Clock.fixed(now, zone);
	}

	@Override
	public Instant instant() {
		return now;
	}
}
