package com.blinkerleague.server.point.application.port.out;

import com.blinkerleague.server.point.domain.PointEntry;

public interface SavePointEntryPort {

	void saveNew(PointEntry entry);
}
