package com.blinkerleague.server.auth.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.auth.application.service.TokenReissueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final TokenReissueService tokenReissueService;

	@SecurityRequirements // 공개 API: 인증 없음
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
