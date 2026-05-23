package io.weblinkpilot.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.weblinkpilot", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule modules_should_be_free_of_cycles =
            slices().matching("io.weblinkpilot.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule web_layers_should_not_depend_on_repositories =
            classes().that().resideInAPackage("io.weblinkpilot..web..")
                    .should().onlyDependOnClassesThat(
                            resideInAPackage("io.weblinkpilot..web..")
                                    .or(resideInAPackage("io.weblinkpilot..service.."))
                                    .or(resideInAPackage("io.weblinkpilot..exception.."))
                                    .or(resideInAPackage("io.weblinkpilot.shared.contracts.."))
                                    .or(resideInAPackage("java.."))
                                    .or(resideInAPackage("jakarta.."))
                                    .or(resideInAPackage("org.springframework.."))
                                    .or(resideInAPackage("io.swagger.v3.oas.annotations.."))
                                    .or(resideInAPackage("io.micrometer.."))
                                    .or(resideInAPackage("org.slf4j.."))
                    );

    @ArchTest
    static final ArchRule domain_should_stay_pure =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("io.weblinkpilot..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("io.weblinkpilot..web..", "io.weblinkpilot..service..", "io.weblinkpilot..repository..");

    @ArchTest
    static final ArchRule shared_contracts_should_stay_isolated =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("io.weblinkpilot.shared.contracts..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("io.weblinkpilot..web..", "io.weblinkpilot..service..", "io.weblinkpilot..repository..", "io.weblinkpilot..config..");
}
