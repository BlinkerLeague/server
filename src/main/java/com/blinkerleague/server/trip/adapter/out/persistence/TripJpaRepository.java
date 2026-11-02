package com.blinkerleague.server.trip.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blinkerleague.server.trip.domain.Trip;

interface TripJpaRepository extends JpaRepository<Trip, String> {
}
