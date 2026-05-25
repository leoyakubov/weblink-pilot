package io.weblinkpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "io.weblinkpilot")
@EnableAsync
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
