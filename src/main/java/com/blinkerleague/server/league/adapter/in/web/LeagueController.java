package com.blinkerleague.server.league.adapter.in.web;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.league.application.service.GetCurrentLeagueService;
import com.blinkerleague.server.league.application.service.GetCurrentLeagueService.CurrentLeague;
import com.blinkerleague.server.league.domain.LeagueBoard.RegionBoard;
import com.blinkerleague.server.league.domain.LeagueSeason;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "지역 리그", description = "시즌(달력 월) 지역 리그 순위와 지역 안전지수")
@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
public class LeagueController {

	private final GetCurrentLeagueService getCurrentLeagueService;

	@Operation(summary = "이번 시즌 내 리그 조회",
		description = "이번 시즌(한국 시간 달력 월, D29)의 내 리그 권역 순위, 내 순위, 모든 권역의 지역 안전지수를 돌려준다(SCR-04). "
			+ "점수는 공통 항목만으로 매긴 리그 점수(D19)의 거리 가중 평균이다. 순위에는 시즌 누적 50km 또는 운행 10회 이상인 운전자만 오르고(D13), "
			+ "리그 점수가 있는 참여자(순위 공개를 끈 운전자 포함)가 10명 미만인 권역은 순위를 공개하지 않는다(D11). "
			+ "순위 공개를 끈 운전자는 순위 목록 · 안전지수에서 빠지고 나머지 운전자끼리 순위를 매긴다(D8). "
			+ "응답에는 닉네임만 담는다.")
	@ApiResponse(responseCode = "200", description = "이번 시즌 리그 현황. 등록 지역이 매핑표에 없으면 league는 null")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/current")
	public CurrentLeagueResponse current(@CurrentDriver Long driverId) {
		return CurrentLeagueResponse.from(getCurrentLeagueService.current(driverId));
	}

	public record CurrentLeagueResponse(SeasonResponse season, LeagueResponse league, MeResponse me,
		List<RegionResponse> regions) {

		static CurrentLeagueResponse from(CurrentLeague current) {
			return new CurrentLeagueResponse(SeasonResponse.from(current.season()),
				current.league() == null ? null : LeagueResponse.from(current.league()),
				new MeResponse(current.me().visible(), current.me().eligible(), current.me().score(),
					current.me().rank(), current.me().trips(), current.me().distanceKm()),
				current.regions().stream().map(RegionResponse::from).toList());
		}
	}

	public record SeasonResponse(String seasonId, OffsetDateTime startsAt, OffsetDateTime endsAt) {

		static SeasonResponse from(LeagueSeason season) {
			return new SeasonResponse(season.id(), kst(season.startsAt()), kst(season.endsAt()));
		}
	}

	@Schema(description = "내 리그 권역. rankings는 순위가 공개됐을 때만 상위 N명(기준값 league.topN)")
	public record LeagueResponse(String leagueCode, String name, int participants, boolean rankingPublished,
		Double safetyIndex, Integer rank, List<RankingResponse> rankings) {

		static LeagueResponse from(RegionBoard board) {
			return new LeagueResponse(board.region().code(), board.region().name(), board.participants(),
				board.published(), board.safetyIndex(), board.rank(),
				board.rankings().stream().map(r -> new RankingResponse(r.rank(), r.nickname(), r.score())).toList());
		}
	}

	public record RankingResponse(int rank, String nickname, double score) {
	}

	@Schema(description = "내 시즌 기록. rank는 순위가 공개되고 자격(D13)이 있고 순위 공개를 켰을 때만 있다")
	public record MeResponse(boolean visible, boolean eligible, Double score, Integer rank, int trips,
		double distanceKm) {
	}

	@Schema(description = "월드맵용 권역별 지역 안전지수(D5)와 권역 순위. 리그 점수가 없는 권역은 null")
	public record RegionResponse(String leagueCode, String name, Double safetyIndex, Integer rank) {

		static RegionResponse from(RegionBoard board) {
			return new RegionResponse(board.region().code(), board.region().name(), board.safetyIndex(), board.rank());
		}
	}

	private static OffsetDateTime kst(Instant instant) {
		return OffsetDateTime.ofInstant(instant, TimeConfig.ZONE);
	}
}
