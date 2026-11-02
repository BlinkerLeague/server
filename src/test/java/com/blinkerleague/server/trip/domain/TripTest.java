package com.blinkerleague.server.trip.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.threshold.domain.ScoreItem;

class TripTest {

	private static final Instant STARTED_AT = Instant.parse("2026-11-03T08:00:00Z");
	private static final double MIN_DISTANCE_KM = 3.0;

	@ParameterizedTest(name = "시작 후 {0}초에 종료")
	@ValueSource(longs = {0, -60})
	void 종료_시각이_시작_시각보다_뒤가_아니면_INVALID_INPUT으로_거부한다(long secondsAfterStart) {
		Instant endedAt = STARTED_AT.plusSeconds(secondsAfterStart);

		assertThatThrownBy(() -> Trip.upload("trip-1", 1L, STARTED_AT, endedAt, 12.3,
			Set.of(ScoreItem.TURN_SIGNAL), List.of(), MIN_DISTANCE_KM))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_INPUT);
	}
}
