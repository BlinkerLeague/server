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
 * 시즌의 리그 소속(D12). 그 시즌에 처음 참여 운행을 올릴 때 등록 지역으로 정하고, 시즌 동안 바꾸지 않는다.
 * 시즌 중 지역을 바꾸면 다음 시즌부터 반영된다.
 */
@Entity
@Table(name = "league_member",
	uniqueConstraints = @UniqueConstraint(name = "uk_league_member_season_driver",
		columnNames = {"season_id", "driver_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "league_member_id")
	private Long id;

	@Column(name = "season_id", nullable = false, length = 7)
	private String seasonId;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "league_code", nullable = false, length = 30)
	private String leagueCode;

	public LeagueMember(String seasonId, Long driverId, String leagueCode) {
		this.seasonId = seasonId;
		this.driverId = driverId;
		this.leagueCode = leagueCode;
	}
}
