package com.blinkerleague.server.score.domain;

/**
 * 점수에 쓰는 이벤트 payload 값(plan.md 4.1). 앱이 보내지 않은 값은 null이다.
 * - TURN: signalOn(P0 호환), signalTiming, maxSpeedKmh
 * - NIGHT_SEGMENT · RAIN_SEGMENT: durationSec, headlightOffSec / RAIN_SEGMENT: knownLimitSec, overRainLimitSec
 * - SEAT_BELT: buckledAtDeparture, unbuckledMovingSec
 */
public record EventPayload(boolean signalOn, SignalTiming signalTiming, Double maxSpeedKmh, Integer durationSec,
	Integer headlightOffSec, Integer knownLimitSec, Integer overRainLimitSec, Boolean buckledAtDeparture,
	Integer unbuckledMovingSec) {

	public static final EventPayload EMPTY = new EventPayload(false, null, null, null, null, null, null, null, null);

	public static EventPayload turn(boolean signalOn) {
		return new EventPayload(signalOn, null, null, null, null, null, null, null, null);
	}
}
