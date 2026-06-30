package io.weblinkpilot.platform.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

  private static final String FLYWAY_BEAN_NAME = "flyway";
  private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";
  private static final String MIGRATIONS_LOCATION = "classpath:db/migration";

  @Bean(initMethod = "migrate")
  @ConditionalOnMissingBean(Flyway.class)
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).locations(MIGRATIONS_LOCATION).load();
  }

  @Bean
  public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
    return beanFactory -> {
      if (beanFactory.containsBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
        BeanDefinition definition = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN_NAME);
        definition.setDependsOn(FLYWAY_BEAN_NAME);
      }
    };
  }
}
