package com.blinkerleague.server.driver.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blinkerleague.server.driver.application.service.RegisterDriverService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

	private final RegisterDriverService registerDriverService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterDriverResponse register(@RequestBody RegisterDriverRequest request) {
		RegisterDriverService.Result result = registerDriverService.register(
			new RegisterDriverService.Command(request.nickname(), request.regionCode()));
		return new RegisterDriverResponse(result.driverId(), result.nickname(), result.regionCode(),
			result.accessToken(), result.refreshToken());
	}

	public record RegisterDriverRequest(String nickname, String regionCode) {
	}

	public record RegisterDriverResponse(Long driverId, String nickname, String regionCode, String accessToken,
		String refreshToken) {
	}
}
