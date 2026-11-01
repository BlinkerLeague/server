package com.blinkerleague.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** CLAUDE.md의 헥사고날 규칙(의존 방향 adapter → application → domain)을 빌드에서 강제한다. */
class HexagonalArchitectureTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("com.blinkerleague.server");

	@Test
	void application은_adapter에_의존하지_않는다() {
		noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..adapter..")
			.allowEmptyShould(true)
			.check(CLASSES);
	}

	@Test
	void domain은_application과_adapter에_의존하지_않는다() {
		noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
			.allowEmptyShould(true)
			.check(CLASSES);
	}

	@Test
	void application은_웹_Redis_JWT_보안설정을_직접_쓰지_않는다() {
		noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAnyPackage(
				"org.springframework.web..",
				"org.springframework.data.redis..",
				"org.springframework.security.oauth2..",
				"jakarta.servlet..",
				"com.blinkerleague.server.security..")
			.allowEmptyShould(true)
			.check(CLASSES);
	}
}
