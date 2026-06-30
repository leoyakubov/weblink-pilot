package io.weblinkpilot.platform.async;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

  private static final String DEFAULT_THREAD_NAME_PREFIX = "weblink-async-";

  @Min(1)
  private int corePoolSize = 2;

  @Min(1)
  private int maxPoolSize = 4;

  @Min(0)
  private int queueCapacity = 100;

  private String threadNamePrefix = DEFAULT_THREAD_NAME_PREFIX;

  public int getCorePoolSize() {
    return corePoolSize;
  }

  public void setCorePoolSize(int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  public int getQueueCapacity() {
    return queueCapacity;
  }

  public void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  public String getThreadNamePrefix() {
    return threadNamePrefix;
  }

  public void setThreadNamePrefix(String threadNamePrefix) {
    this.threadNamePrefix =
        threadNamePrefix == null || threadNamePrefix.isBlank()
            ? DEFAULT_THREAD_NAME_PREFIX
            : threadNamePrefix;
  }
}
