package com.blinkerleague.server.driver.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.domain.Driver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DriverPersistenceAdapter implements SaveDriverPort, LoadDriverPort {

	private final DriverJpaRepository repository;

	@Override
	public Driver save(Driver driver) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}

	@Override
	public boolean existsByNickname(String nickname) {
		throw new UnsupportedOperationException("아직 구현되지 않음");
	}
}
