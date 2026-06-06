package io.weblinkpilot.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static org.springframework.modulith.core.ApplicationModules.of;

import io.weblinkpilot.Application;
import org.junit.jupiter.api.Test;

class ModulithStructureTest {

  @Test
  void applicationModulesShouldVerifyForBusinessPackages() {
    of(
            Application.class,
            resideInAPackage("io.weblinkpilot.bootstrap..")
                .or(resideInAPackage("io.weblinkpilot.config..")))
        .verify();
  }
}
