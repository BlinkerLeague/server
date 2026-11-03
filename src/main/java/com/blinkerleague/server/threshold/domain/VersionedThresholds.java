package com.blinkerleague.server.threshold.domain;

/** 기준값과 그 버전. 앱은 version이 바뀐 경우에만 다시 내려받는다. */
public record VersionedThresholds(long version, Thresholds thresholds) {
}
