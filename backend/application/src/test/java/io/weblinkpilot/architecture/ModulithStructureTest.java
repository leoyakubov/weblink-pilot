package io.weblinkpilot.architecture;

import static org.springframework.modulith.core.ApplicationModules.of;

import io.weblinkpilot.Application;
import org.junit.jupiter.api.Test;

class ModulithStructureTest {

  @Test
  void applicationModulesShouldVerifyForBusinessPackages() {
    System.setProperty("spring.modulith.detection-strategy", "explicitly-annotated");
    of(Application.class).verify();
  }
}
