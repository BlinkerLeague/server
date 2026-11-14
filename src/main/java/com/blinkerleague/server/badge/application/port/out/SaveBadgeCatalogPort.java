package com.blinkerleague.server.badge.application.port.out;

import java.util.List;

import com.blinkerleague.server.badge.domain.Badge;

public interface SaveBadgeCatalogPort {

	boolean existsAny();

	void saveAll(List<Badge> badges);
}
