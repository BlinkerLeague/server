package com.blinkerleague.server.league.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.service.SettleSeasonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시즌(달력 월, D29)이 끝나면 지난 시즌을 정산한다(D30). 서버 1대 전제다. 여러 대면 분산 잠금이 필요하지만
 * 우승 칭호는 운전자 · 시즌마다 하나, 배지는 운전자마다 하나라 중복 실행돼도 두 번 생기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueSeasonScheduler {

	private final SettleSeasonService settleSeasonService;

	@Scheduled(cron = "0 10 0 1 * *", zone = "Asia/Seoul")
	public void run() {
		int champions = settleSeasonService.settleLastSeason();
		log.info("지난 시즌 리그를 정산해 {}명에게 우승 칭호를 남겼습니다.", champions);
	}
}
