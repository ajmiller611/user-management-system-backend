package io.github.ajmiller611.usermanagement.config;

import io.github.ajmiller611.usermanagement.service.BootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class responsible for application startup initialization.
 *
 * <p>Defines a {@link CommandLineRunner} bean that runs when the application starts
 * and delegates initialization logic to {@link BootstrapService}.</p>
 *
 * <p>This setup ensures required roles and the default admin user exist
 * without placing startup logic directly in the main application class.</p>
 */
@Configuration
@RequiredArgsConstructor
public class BootstrapRunnerConfig {

  private final BootstrapService bootstrapService;

  /**
   * Executes application bootstrap logic at startup.
   *
   * @return a {@link CommandLineRunner} that invokes initialization logic
   */
  @Bean
  CommandLineRunner initialize() {
    return args -> bootstrapService.initialize();
  }
}
