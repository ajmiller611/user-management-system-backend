package com.logistics.military;

import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Entry point for the Military Logistics application.
 *
 * <p>This class is the main class for the Spring Boot application.
 * It initializes the Spring application context and launches the application.
 * It creates default roles(e.g. "ADMIN", "USER") and an Admin user.
 * </p>
 */
@RequiredArgsConstructor
@SpringBootApplication
public class MilitaryLogisticsApplication {

  private final Environment env;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Main method that serves as the entry point for the application.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(MilitaryLogisticsApplication.class, args);
  }

  /**
   * CommandLineRunner bean returns a lambda expression to execute instructions
   * for initializing the database with default roles and a user with "ADMIN" role.
   *
   * <p>This bean is executed when the application starts. It checks if the "ADMIN" role already
   * exists in the RoleRepository. If not, it creates the "ADMIN" and "USER" roles, then creates
   * a default admin user with the "ADMIN" role, and saves both the roles and the user to the
   * database.
   * </p>
   *
   * @param roleRepository The repository used to interact with the roles in the database.
   * @param logisticsUserRepository The repository used to interact with the logistics users
   *                                in the database.
   * @param passwordEncoder The password encoder used to encode the user's password.
   * @return A lambda expression representing the execution of instructions to initialize
   *         the database with roles and an admin user if they do not already exist.
   */
  @Bean
  @Profile("dev")
  CommandLineRunner run(
      RoleRepository roleRepository,
      LogisticsUserRepository logisticsUserRepository,
      PasswordEncoder passwordEncoder,
      Clock clock
  ) {

    return args -> {
      if (roleRepository.findByAuthority("ADMIN").isPresent()) {
        return;
      }

      // Create an Admin role and save it to the database.
      Role adminRole = roleRepository.save(new Role("ADMIN"));
      logger.info("Role created: {}", adminRole);
      Role userRole = roleRepository.save(new Role("USER"));
      logger.info("Role created: {}", userRole);

      Set<Role> roles = new HashSet<>();
      roles.add(adminRole);
      roles.add(userRole);

      String passwordFromEnv = env.getProperty("ADMIN_PASSWORD");

      if (passwordFromEnv == null || passwordFromEnv.isBlank()) {
        throw new IllegalStateException("ADMIN_PASSWORD is not set");
      }

      // Create a user with admin role.
      LogisticsUser admin = new LogisticsUser(
          1L,
          "admin",
          passwordEncoder.encode(passwordFromEnv),
          "admin@example.com",
          LocalDateTime.now(clock),
          roles
      );
      logisticsUserRepository.save(admin);
      logger.info("Admin User created: {}", admin);
    };
  }
}
