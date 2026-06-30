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
      classes()
          .that()
          .resideInAPackage("io.weblinkpilot..web..")
          .should()
          .onlyDependOnClassesThat(
              resideInAPackage("io.weblinkpilot..web..")
                  .or(resideInAPackage("io.weblinkpilot..config.."))
                  .or(resideInAPackage("io.weblinkpilot..criteria.."))
                  .or(resideInAPackage("io.weblinkpilot..service.."))
                  .or(resideInAPackage("io.weblinkpilot..session.."))
                  .or(resideInAPackage("io.weblinkpilot..exception.."))
                  .or(resideInAPackage("io.weblinkpilot..support.."))
                  .or(resideInAPackage("io.weblinkpilot..qr.."))
                  .or(resideInAPackage("io.weblinkpilot..token.."))
                  .or(resideInAPackage("io.weblinkpilot.shared.api.."))
                  .or(resideInAPackage("io.weblinkpilot.shared.events.."))
                  .or(resideInAPackage("io.weblinkpilot.shared.ports.."))
                  .or(resideInAPackage("io.weblinkpilot.shared.types.."))
                  .or(resideInAPackage("java.."))
                  .or(resideInAPackage("jakarta.."))
                  .or(resideInAPackage("org.springframework.."))
                  .or(resideInAPackage("io.swagger.v3.oas.annotations.."))
                  .or(resideInAPackage("io.micrometer.."))
                  .or(resideInAPackage("org.slf4j.."))
                  .or(resideInAPackage("edu.umd.cs.findbugs.annotations..")));

  @ArchTest
  static final ArchRule domain_should_stay_pure =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot..web..",
              "io.weblinkpilot..service..",
              "io.weblinkpilot..repository..");

  @ArchTest
  static final ArchRule shared_module_should_stay_isolated =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot.shared..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot.platform..",
              "io.weblinkpilot..config..",
              "io.weblinkpilot..web..",
              "io.weblinkpilot..service..",
              "io.weblinkpilot..repository..");

  @ArchTest
  static final ArchRule auth_should_not_import_other_business_modules =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot.auth..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot.links..", "io.weblinkpilot.analytics..", "io.weblinkpilot.ai..");

  @ArchTest
  static final ArchRule links_should_not_import_other_business_modules =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot.links..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot.auth..", "io.weblinkpilot.analytics..", "io.weblinkpilot.ai..");

  @ArchTest
  static final ArchRule analytics_should_not_import_other_business_modules =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot.analytics..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot.auth..", "io.weblinkpilot.links..", "io.weblinkpilot.ai..");

  @ArchTest
  static final ArchRule ai_should_not_import_other_business_modules =
      com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
          .that()
          .resideInAPackage("io.weblinkpilot.ai..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.weblinkpilot.auth..", "io.weblinkpilot.links..", "io.weblinkpilot.analytics..");
}
