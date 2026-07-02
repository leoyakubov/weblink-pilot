package io.weblinkpilot.platform.persistence;

import com.zaxxer.hikari.HikariDataSource;
import io.weblinkpilot.platform.PlatformProfiles;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(PlatformProfiles.DEMO_EPHEMERAL)
public class DemoEphemeralDataSourceConfiguration {

  private static final String H2_DEMO_URL =
      "jdbc:h2:mem:weblinkpilot_demo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

  @Bean
  @Primary
  public DataSource demoEphemeralDataSource() {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(H2_DEMO_URL);
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    dataSource.setDriverClassName("org.h2.Driver");
    return dataSource;
  }
}
