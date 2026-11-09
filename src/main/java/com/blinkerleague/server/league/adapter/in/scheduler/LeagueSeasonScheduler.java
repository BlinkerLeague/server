package com.blinkerleague.server.league.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.blinkerleague.server.league.application.service.SettleSeasonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시즌(달력 월, D29)이 끝나면 지난 시즌을 정산한다(D30). 서버 1대 전제다. 여러 대면 분산 잠금이 필요하지만
 * 보상은 운전자 · 시즌마다 한 번만 적립되므로 중복 실행돼도 두 번 적립되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueSeasonScheduler {

	private final SettleSeasonService settleSeasonService;

	@Scheduled(cron = "0 10 0 1 * *", zone = "Asia/Seoul")
	public void run() {
		int rewarded = settleSeasonService.settleLastSeason();
		log.info("지난 시즌 리그를 정산해 {}명에게 시즌 보상을 요청했습니다.", rewarded);
	}
}
