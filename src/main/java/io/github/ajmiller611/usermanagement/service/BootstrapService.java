package io.github.ajmiller611.usermanagement.service;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for bootstrapping the system with required baseline data.
 *
 * <p>This ensures the application has the minimum required data to start and operate,
 * including required roles and a default administrator account.</p>
 */
@Service
@RequiredArgsConstructor
public class BootstrapService {

  private final Environment env;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String ADMIN = "ADMIN";
  private static final String USER = "USER";

  /**
   * Initializes the system by ensuring required roles and an administrator account exist.
   *
   * <p>This method should be called during application startup to guarantee that the system
   * is in a valid and usable state. It enforces the correct order of operations by ensuring
   * roles are created before attempting to create the admin user.</p>
   */
  public void initialize() {
    ensureRolesExist();
    ensureAdminExists();
  }

  /**
   * Ensures that the required roles ("ADMIN" and "USER") exist in the system.
   * Creates them if they are missing.
   */
  public void ensureRolesExist() {
    if (roleRepository.findByAuthority(ADMIN).isEmpty()) {
      Role adminRole = roleRepository.save(new Role(ADMIN));
      logger.info("Role created: {}", adminRole);
    }

    if (roleRepository.findByAuthority(USER).isEmpty()) {
      Role userRole = roleRepository.save(new Role(USER));
      logger.info("Role created: {}", userRole);
    }
  }

  /**
   * Ensures that a default administrator user exists in the system.
   *
   * <p>If a user with the username "admin" does not exist, a new admin user is created
   * with both "ADMIN" and "USER" roles. If the user already exists, no action is taken.</p>
   *
   * @throws IllegalStateException if required roles are not found
   * @throws IllegalArgumentException if the ADMIN_PASSWORD environment variable is not set
   */
  public void ensureAdminExists() {
    if (userRepository.findByUsername("admin").isPresent()) {
      return;
    }

    Role adminRole = roleRepository.findByAuthority(ADMIN)
        .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
    Role userRole = roleRepository.findByAuthority(USER)
        .orElseThrow(() -> new IllegalStateException("USER role not found"));

    Set<Role> roles = Set.of(adminRole, userRole);

    String passwordFromEnv = env.getProperty("ADMIN_PASSWORD");
    if (passwordFromEnv == null || passwordFromEnv.isBlank()) {
      throw new IllegalArgumentException("ADMIN_PASSWORD is not set");
    }

    User admin = new User(
        null,
        "admin",
        passwordEncoder.encode(passwordFromEnv),
        "admin@example.com",
        LocalDateTime.now(clock),
        roles
    );

    userRepository.save(admin);
    logger.info("Admin created: {}", admin);
  }
}
