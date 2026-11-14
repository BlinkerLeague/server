package com.blinkerleague.server.point.application.port.out;

import com.blinkerleague.server.point.domain.PointEntry;
import com.blinkerleague.server.point.domain.PointReason;

public interface SavePointEntryPort {

	/** 같은 사유 · refId의 적립이 이미 있으면 {@link PointAlreadyEarnedException}. */
	void saveNew(PointEntry entry);

	class PointAlreadyEarnedException extends RuntimeException {

		public PointAlreadyEarnedException(PointReason reason, String refId, Throwable cause) {
			super("이미 적립했습니다: " + reason + " " + refId, cause);
		}
	}
}
