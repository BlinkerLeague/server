package com.blinkerleague.server.league.domain;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시즌 우승 칭호(D30). 끝난 시즌의 안전지수 1위 권역 순위 자격자에게 운전자 · 시즌마다 하나 남긴다.
 * 포인트 대신 주는 보상이라(D34) 시즌마다 쌓인다.
 */
@Entity
@Table(name = "league_champion",
	uniqueConstraints = @UniqueConstraint(name = "uk_league_champion_season_driver",
		columnNames = {"season_id", "driver_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonChampion extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "league_champion_id")
	private Long id;

	@Column(name = "season_id", nullable = false, length = 7)
	private String seasonId;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "league_code", nullable = false, length = 30)
	private String leagueCode;

	public SeasonChampion(String seasonId, Long driverId, String leagueCode) {
		this.seasonId = seasonId;
		this.driverId = driverId;
		this.leagueCode = leagueCode;
	}
}
