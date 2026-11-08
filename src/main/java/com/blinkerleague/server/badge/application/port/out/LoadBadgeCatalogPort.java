package com.blinkerleague.server.badge.application.port.out;

import java.util.List;

import com.blinkerleague.server.badge.domain.Badge;

public interface LoadBadgeCatalogPort {

	/** 전체 배지 카탈로그(진열 순서). */
	List<Badge> findAll();
}
