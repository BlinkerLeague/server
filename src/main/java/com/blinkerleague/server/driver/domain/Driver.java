package com.blinkerleague.server.driver.domain;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운전자. 실명 · 연락처 같은 개인정보는 두지 않는다. */
@Getter
@Entity
@Table(name = "driver")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "driver_id")
	private Long id;

	@Column(nullable = false, length = 12)
	private String nickname;

	@Column(name = "region_code", nullable = false, length = 5)
	private String regionCode;

	private Driver(String nickname, String regionCode) {
		this.nickname = nickname;
		this.regionCode = regionCode;
	}

	public static Driver register(String nickname, String regionCode) {
		return new Driver(nickname, regionCode);
	}
}
