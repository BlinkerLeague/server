package com.blinkerleague.server.trip.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.trip.domain.Trip;

interface TripJpaRepository extends JpaRepository<Trip, String> {

	List<Trip> findByDriverIdOrderByEndedAtDesc(Long driverId, Pageable pageable);
}
