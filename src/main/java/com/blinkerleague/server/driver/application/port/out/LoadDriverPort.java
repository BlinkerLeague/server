package com.blinkerleague.server.driver.application.port.out;

import java.util.Optional;

import com.blinkerleague.server.driver.domain.Driver;

public interface LoadDriverPort {

	boolean existsByNickname(String nickname);

	Optional<Driver> findById(Long driverId);
}
