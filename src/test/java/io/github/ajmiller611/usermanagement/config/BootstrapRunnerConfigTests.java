package io.github.ajmiller611.usermanagement.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.ajmiller611.usermanagement.service.BootstrapService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;

/**
 * Unit tests for {@link BootstrapRunnerConfig}.
 *
 * <p>Verifies that the {@link CommandLineRunner} bean correctly delegates
 * application startup initialization to {@link BootstrapService}.</p>
 */
class BootstrapRunnerConfigTests {

  /**
   * Ensures that the CommandLineRunner invokes BootstrapService initialization logic.
   *
   * <p>This test validates that when the runner is executed, it calls
   * {@link BootstrapService#initialize()}, confirming that startup behavior
   * is properly wired.</p>
   */
  @Test
  void testCommandLineRunnerDelegatesToBootstrapService() throws Exception {
    BootstrapService bootstrapService = mock(BootstrapService.class);

    BootstrapRunnerConfig config = new BootstrapRunnerConfig(bootstrapService);

    CommandLineRunner runner = config.initialize();

    runner.run();

    verify(bootstrapService).initialize();
  }
}
