package com.blinkerleague.server.report.adapter.out.persistence;

import com.blinkerleague.server.common.jpa.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 만든 주간 리포트(plan.md 4.5 weekly_report). 조회는 이 저장본을 그대로 돌려준다. */
@Getter
@Entity
@Table(name = "weekly_report", uniqueConstraints = @UniqueConstraint(name = "uk_weekly_report_driver_week",
	columnNames = {"driver_id", "week_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WeeklyReportEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "weekly_report_id")
	private Long id;

	@Column(name = "driver_id", nullable = false)
	private Long driverId;

	@Column(name = "week_key", nullable = false, length = 8)
	private String weekKey;

	// PostgreSQL에서 @Lob String은 대용량 객체(oid)가 되므로 text로 매핑한다(docs/deployment.md 4.2).
	@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
	@Column(name = "report_json", nullable = false)
	private String reportJson;

	WeeklyReportEntity(Long driverId, String weekKey, String reportJson) {
		this.driverId = driverId;
		this.weekKey = weekKey;
		this.reportJson = reportJson;
	}
}
