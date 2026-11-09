package com.blinkerleague.server.league.application.port.out;

import java.util.List;

import com.blinkerleague.server.league.domain.SeasonChampion;

/** 시즌 우승 칭호(D30). 운전자 · 시즌마다 하나다. */
public interface SeasonChampionStore {

	/** 저장한다. 같은 운전자 · 시즌이 이미 있으면 아무 일도 하지 않는다. */
	void saveIfAbsent(SeasonChampion champion);

	/** 운전자의 우승 칭호, 오래된 시즌부터. */
	List<SeasonChampion> findByDriver(Long driverId);
}
