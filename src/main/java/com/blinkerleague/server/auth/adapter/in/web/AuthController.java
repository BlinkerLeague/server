package com.blinkerleague.server.auth.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.application.service.TokenReissueService;
import com.blinkerleague.server.common.error.ErrorResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증", description = "Access · Refresh 토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final TokenReissueService tokenReissueService;

	@Operation(summary = "토큰 재발급",
		description = "Refresh 토큰을 본문으로 보내 새 Access · Refresh 토큰을 받는다. 보낸 Refresh 토큰은 무효가 되므로 응답의 새 토큰으로 바꿔 저장한다.")
	@SecurityRequirements // 공개 API: 인증 없음
	@ApiResponse(responseCode = "200", description = "새 Access · Refresh 토큰. 사용한 Refresh 토큰은 무효가 된다")
	@ApiResponse(responseCode = "400", description = "INVALID_INPUT: refreshToken 누락",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "INVALID_TOKEN: 없거나 만료 · 사용된 Refresh 토큰. 사용된 토큰을 다시 쓰면 그 운전자의 토큰 전체가 무효가 된다",
		content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		TokenReissueService.TokenPair pair = tokenReissueService.reissue(request.refreshToken());
		return new TokenResponse(pair.accessToken(), pair.refreshToken());
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record TokenResponse(String accessToken, String refreshToken) {
	}
}
