package com.blinkerleague.server.driver.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort.DriverTokens;
import com.blinkerleague.server.driver.domain.Driver;

class RegisterDriverServiceTest {

	private final InMemoryDriverRepository drivers = new InMemoryDriverRepository();
	private final RegisterDriverService service = new RegisterDriverService(drivers,
		driverId -> new DriverTokens("access-for-" + driverId, "refresh-for-" + driverId));

	@Test
	void 운전자를_등록하면_저장하고_driverId와_액세스_리프레시_토큰을_돌려준다() {
		RegisterDriverService.Result result = service.register(new RegisterDriverService.Command("깜빡왕", "11680"));

		assertThat(result.driverId()).isNotNull();
		assertThat(result.nickname()).isEqualTo("깜빡왕");
		assertThat(result.regionCode()).isEqualTo("11680");
		assertThat(result.accessToken()).isEqualTo("access-for-" + result.driverId());
		assertThat(result.refreshToken()).isEqualTo("refresh-for-" + result.driverId());
		assertThat(drivers.findAll()).extracting(Driver::getNickname).containsExactly("깜빡왕");
	}
}
