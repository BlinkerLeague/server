package com.blinkerleague.server.driver.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.blinkerleague.server.common.error.BusinessException;
import com.blinkerleague.server.common.error.ErrorCode;
import com.blinkerleague.server.driver.application.port.out.IssueDriverTokensPort.DriverTokens;
import com.blinkerleague.server.driver.domain.Driver;

class RegisterDriverServiceTest {

	private final InMemoryDriverRepository drivers = new InMemoryDriverRepository();
	private final RegisterDriverService service = new RegisterDriverService(drivers, drivers,
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

	@Test
	void 이미_사용_중인_닉네임이면_CONFLICT로_거부하고_저장하지_않는다() {
		service.register(new RegisterDriverService.Command("깜빡왕", "11680"));

		assertThatThrownBy(() -> service.register(new RegisterDriverService.Command("깜빡왕", "26350")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(ErrorCode.CONFLICT);
		assertThat(drivers.findAll()).hasSize(1);
	}
}
