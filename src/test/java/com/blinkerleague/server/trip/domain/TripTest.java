package com.blinkerleague.server.trip.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

	@ParameterizedTest(name = "시작 후 {0}초 이벤트 → 허용 {1}")
	@CsvSource({"-1, false", "0, true", "1200, true", "2400, true", "2401, false"})
	void 이벤트_시각은_시작부터_종료까지_구간_안이어야_하고_밖이면_INVALID_INPUT으로_거부한다(long secondsAfterStart,
		boolean allowed) {
		Instant endedAt = STARTED_AT.plusSeconds(2400);
		List<RecordedEvent> events = List.of(
			new RecordedEvent("event-1", TripEventType.TURN, STARTED_AT.plusSeconds(secondsAfterStart), null));

		ThrowingCallable upload = () -> Trip.upload("trip-1", 1L, STARTED_AT, endedAt, 12.3,
			Set.of(ScoreItem.TURN_SIGNAL), events, MIN_DISTANCE_KM);

		if (allowed) {
			assertThatCode(upload).doesNotThrowAnyException();
		} else {
			assertThatThrownBy(upload)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
		}
	}
}
