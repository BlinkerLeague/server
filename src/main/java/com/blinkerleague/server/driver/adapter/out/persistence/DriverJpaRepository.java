package com.blinkerleague.server.driver.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.driver.domain.Driver;

interface DriverJpaRepository extends JpaRepository<Driver, Long> {

	boolean existsByNickname(String nickname);
}
