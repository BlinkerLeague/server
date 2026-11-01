package com.blinkerleague.server.auth.adapter.in.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 컨트롤러 인자에 붙이면 인증된 운전자의 driverId(Access 토큰의 {@code sub})를 받는다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentDriver {
}
