package com.blinkerleague.server.threshold.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface AppConfigJpaRepository extends JpaRepository<AppConfigEntity, String> {
}
