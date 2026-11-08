package com.blinkerleague.server.badge.application.port.out;

import com.blinkerleague.server.badge.domain.BadgeProgress;

public interface BadgeProgressPort {

	BadgeProgress progress(Long driverId);
}
