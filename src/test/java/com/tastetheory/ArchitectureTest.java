package com.tastetheory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The layering rules, as tests.
 *
 * <p>Every rule here is also written in prose in the relevant {@code
 * package-info.java}. The difference is that these fail the build.
 */
@AnalyzeClasses(packages = "com.tastetheory", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule domain_is_free_of_frameworks = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..",
					"jakarta.persistence..",
					"jakarta.transaction..",
					"org.hibernate..",
					"com.fasterxml.jackson..")
			.because("the domain model must be plain Java, runnable and testable with no framework present");

	@ArchTest
	static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
			.because("dependencies point inward: the innermost layer knows nothing of the ones around it");

	@ArchTest
	static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..infrastructure..")
			.because("the application layer declares ports; infrastructure implements them, never the reverse");

	@ArchTest
	static final ArchRule shared_does_not_depend_on_any_module = noClasses()
			.that().resideInAPackage("com.tastetheory.shared..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"com.tastetheory.inventory..",
					"com.tastetheory.recipes..",
					"com.tastetheory.invoicing..")
			.because("shared value objects are the foundation the modules build on, so they cannot build on a module");
}
