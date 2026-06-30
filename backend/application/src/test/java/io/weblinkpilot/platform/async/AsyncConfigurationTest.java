package io.weblinkpilot.platform.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class AsyncConfigurationTest {

  @Test
  void exposesApplicationAsyncExecutor() {
    AsyncConfiguration configuration = new AsyncConfiguration(new AsyncProperties());

    Executor executor = configuration.applicationAsyncExecutor();

    assertThat(executor).isNotNull();
  }
}
