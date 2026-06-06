package io.weblinkpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.weblinkpilot")
@EnableAsync
@EnableScheduling
@Modulithic(systemName = "WebLinkPilot", sharedModules = "shared")
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
