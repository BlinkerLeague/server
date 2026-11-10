package com.blinkerleague.server.threshold.adapter.out.persistence;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 설정 키 하나에 JSON 값 하나. `value`는 H2 예약어라 컬럼 이름을 `config_value`로 쓴다. */
@Getter
@Entity
@Table(name = "app_config")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AppConfigEntity extends BaseTimeEntity {

	@Id
	@Column(name = "config_key", length = 50)
	private String key;

	// PostgreSQL에서 @Lob String은 대용량 객체(oid)가 되므로 text로 매핑한다(docs/deployment.md 4.2).
	@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
	@Column(name = "config_value", nullable = false)
	private String value;

	@Column(nullable = false)
	private long version;

	AppConfigEntity(String key, String value, long version) {
		this.key = key;
		this.value = value;
		this.version = version;
	}

	void change(String value, long version) {
		this.value = value;
		this.version = version;
	}
}
