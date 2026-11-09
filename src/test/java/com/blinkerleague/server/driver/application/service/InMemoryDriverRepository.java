package com.blinkerleague.server.driver.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.test.util.ReflectionTestUtils;

import com.blinkerleague.server.driver.application.port.out.LoadDriverPort;
import com.blinkerleague.server.driver.application.port.out.SaveDriverPort;
import com.blinkerleague.server.driver.domain.Driver;

/** 단위 테스트용 운전자 저장소. 저장할 때 id를 1부터 순서대로 채운다. */
class InMemoryDriverRepository implements SaveDriverPort, LoadDriverPort {

	private final AtomicLong sequence = new AtomicLong();
	private final List<Driver> drivers = new ArrayList<>();

	@Override
	public Driver save(Driver driver) {
		ReflectionTestUtils.setField(driver, "id", sequence.incrementAndGet());
		drivers.add(driver);
		return driver;
	}

	@Override
	public boolean existsByNickname(String nickname) {
		return drivers.stream().anyMatch(driver -> driver.getNickname().equals(nickname));
	}

	@Override
	public Optional<Driver> findById(Long driverId) {
		return drivers.stream().filter(driver -> driver.getId().equals(driverId)).findFirst();
	}

	List<Driver> findAll() {
		return List.copyOf(drivers);
	}
}
