package com.blinkerleague.server.badge.application.port.out;

import java.util.List;

import com.blinkerleague.server.badge.domain.BadgeAward;

public interface BadgeAwardStore {

	List<BadgeAward> findByDriver(Long driverId);

	/** 같은 운전자 · 배지가 이미 있으면(동시 판정) 저장하지 않고 넘어간다. */
	void saveIfAbsent(BadgeAward award);
}
