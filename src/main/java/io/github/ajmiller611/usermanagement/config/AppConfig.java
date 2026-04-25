package io.github.ajmiller611.usermanagement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for application-wide beans.
 *
 * <p>This class defines shared bean configurations that can be injected
 * into other components within the application context.
 * </p>
 */
@Configuration
@EnableAspectJAutoProxy
public class AppConfig {

  /**
   * Provides a {@link Clock} bean set to the system's default time zone.
   *
   * @return a {@link Clock} instance set to the system's default time zone
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
