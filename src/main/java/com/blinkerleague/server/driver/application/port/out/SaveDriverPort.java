package com.blinkerleague.server.driver.application.port.out;

import com.blinkerleague.server.driver.domain.Driver;

public interface SaveDriverPort {

	/** 저장하고 id가 채워진 운전자를 돌려준다. */
	Driver save(Driver driver);
}
