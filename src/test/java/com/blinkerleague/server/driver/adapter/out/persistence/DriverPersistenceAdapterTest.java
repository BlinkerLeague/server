package com.blinkerleague.server.driver.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.blinkerleague.server.common.jpa.JpaAuditingConfig;
import com.blinkerleague.server.common.time.TimeConfig;
import com.blinkerleague.server.driver.domain.Driver;

@DataJpaTest
@ActiveProfiles("test")
@Import({DriverPersistenceAdapter.class, JpaAuditingConfig.class, TimeConfig.class})
class DriverPersistenceAdapterTest {

	@Autowired
	DriverPersistenceAdapter adapter;

	@Test
	void 저장하면_id가_채워지고_닉네임으로_존재_여부를_확인할_수_있다() {
		Driver saved = adapter.save(Driver.register("깜빡왕", "11680"));

		assertThat(saved.getId()).isNotNull();
		assertThat(adapter.existsByNickname("깜빡왕")).isTrue();
		assertThat(adapter.existsByNickname("없는닉네임")).isFalse();
	}
}
