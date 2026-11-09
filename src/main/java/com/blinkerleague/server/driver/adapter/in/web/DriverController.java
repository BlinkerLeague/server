package com.blinkerleague.server.driver.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.adapter.in.web.CurrentDriver;
import com.blinkerleague.server.common.error.ErrorResponse;
import com.blinkerleague.server.driver.application.service.ChangeLeagueVisibilityService;
import com.blinkerleague.server.driver.application.service.RegisterDriverService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "운전자", description = "운전자 등록")
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

	private final RegisterDriverService registerDriverService;
	private final ChangeLeagueVisibilityService changeLeagueVisibilityService;

	@Operation(summary = "운전자 등록",
		description = "닉네임과 거주 지역(시군구 5자리 코드)으로 운전자를 등록하고 Access · Refresh 토큰을 발급한다. 로그인 API는 없으며, 토큰을 잃으면 다시 등록한다(P0).")
	@SecurityRequirements // 공개 API: 인증 없음
	@ApiResponse(responseCode = "201", description = "등록 완료. Access · Refresh 토큰을 함께 발급한다")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: 닉네임 2~12자, 지역 코드는 시군구 5자리 숫자",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "CONFLICT: 이미 사용 중인 닉네임",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterDriverResponse register(@Valid @RequestBody RegisterDriverRequest request) {
		RegisterDriverService.Result result = registerDriverService.register(
			new RegisterDriverService.Command(request.nickname(), request.regionCode()));
		return new RegisterDriverResponse(result.driverId(), result.nickname(), result.regionCode(),
			result.accessToken(), result.refreshToken());
	}

	@Operation(summary = "리그 순위 공개 설정",
		description = "내 리그 순위 공개를 켜고 끈다(D8, SCR-08). 기본은 공개다. 끄면 리그에서 빠진다: 순위 목록 · 지역 안전지수 · 순위 공개 기준 인원(10명) · 시즌 보상에 들어가지 않는다.")
	@ApiResponse(responseCode = "200", description = "바뀐 설정")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: visible 누락",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 토큰 없음 / INVALID_TOKEN: 위조 · 만료된 토큰",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "NOT_FOUND: 토큰의 운전자가 없음",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/me/league-visibility")
	public LeagueVisibilityResponse changeLeagueVisibility(@CurrentDriver Long driverId,
		@Valid @RequestBody LeagueVisibilityRequest request) {
		return new LeagueVisibilityResponse(changeLeagueVisibilityService.change(driverId, request.visible()));
	}

	public record LeagueVisibilityRequest(@NotNull Boolean visible) {
	}

	public record LeagueVisibilityResponse(boolean visible) {
	}

	public record RegisterDriverRequest(
		@NotBlank @Size(min = 2, max = 12) String nickname,
		// 법정동 코드 앞 5자리(시군구)로 가정한다(plan.md 3.2).
		@NotNull @Pattern(regexp = "\\d{5}") String regionCode) {
	}

	public record RegisterDriverResponse(Long driverId, String nickname, String regionCode, String accessToken,
		String refreshToken) {
	}
}
