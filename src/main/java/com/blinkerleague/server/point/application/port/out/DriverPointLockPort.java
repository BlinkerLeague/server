package com.blinkerleague.server.point.application.port.out;

import java.util.function.Supplier;

/**
 * 같은 운전자의 적립을 한 번에 하나씩 실행한다. 오늘 합계를 읽고 적립하는 사이에
 * 다른 적립이 끼어들면 일일 상한을 넘을 수 있기 때문이다(plan.md 4.2).
 */
public interface DriverPointLockPort {

	<T> T withDriverLock(Long driverId, Supplier<T> work);
}
