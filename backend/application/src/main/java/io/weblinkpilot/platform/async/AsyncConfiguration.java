package io.weblinkpilot.platform.async;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

  private final AsyncProperties asyncProperties;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring singleton configuration stores injected configuration properties.")
  public AsyncConfiguration(AsyncProperties asyncProperties) {
    this.asyncProperties = asyncProperties;
  }

  @Bean(name = "applicationAsyncExecutor")
  public Executor applicationAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix());
    executor.setCorePoolSize(asyncProperties.getCorePoolSize());
    executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
    executor.setQueueCapacity(asyncProperties.getQueueCapacity());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return applicationAsyncExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (throwable, method, args) ->
        log.error(
            "async.task.failed method={} argsCount={} message={}",
            method.getName(),
            args == null ? 0 : args.length,
            throwable.getMessage(),
            throwable);
  }
}
