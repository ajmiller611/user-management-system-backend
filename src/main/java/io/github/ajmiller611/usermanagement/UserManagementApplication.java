package io.github.ajmiller611.usermanagement;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Entry point for the User Management application.
 *
 * <p>This class is the main class for the Spring Boot application.
 * It bootstraps the Spring Boot application and defines startup behavior.
 * </p>
 */
@RequiredArgsConstructor
@SpringBootApplication
public class UserManagementApplication {

  /**
   * Main method that serves as the entry point for the application.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(UserManagementApplication.class, args);
  }
}
