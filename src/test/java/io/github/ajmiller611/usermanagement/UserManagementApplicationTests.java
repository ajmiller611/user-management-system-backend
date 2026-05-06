package io.github.ajmiller611.usermanagement;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class for the application.
 *
 * <p>This class is responsible for running basic application context load tests
 * to verify that the application starts up correctly within the test environment.
 * </p>
 *
 * <p>{@code @ActiveProfiles("test")} activates the "test" profile, loading configuration properties
 * from the application-test.properties file.
 * </p>
 *
 */
@SpringBootTest
@ActiveProfiles("test")
class UserManagementApplicationTests {

  @InjectMocks private UserManagementApplication application;

  /**
   * Test the entry point to the Spring Application.
   */
  @Test
  void testSpringApplicationRun() {
    // Create a mock context for all static methods in SpringApplication class
    try (var mockedSpringApplication = mockStatic(SpringApplication.class)) {
      /*
       * Call main method which calls the mocked version of the run method which doesn't start
       * the Spring Boot application context.
       */
      UserManagementApplication.main(new String[]{});

      // Verify SpringApplication.run method was called
      mockedSpringApplication.verify(() ->
          SpringApplication.run(UserManagementApplication.class, new String[]{}));
    }
  }
}
