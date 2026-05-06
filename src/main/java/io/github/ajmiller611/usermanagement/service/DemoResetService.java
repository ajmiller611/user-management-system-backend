package io.github.ajmiller611.usermanagement.service;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for resetting the application to a predefined demo state.
 *
 * <p>This includes removing existing user data, restoring required system users,
 * and recreating sample users for demonstration and testing purposes.</p>
 */
@Service
@RequiredArgsConstructor
public class DemoResetService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final BootstrapService bootstrapService;

  /**
   * Resets the application to a demo state.
   *
   * <p>This method clears existing user data and user-role mappings,
   * then re-creates the required roles, the default admin user,
   * and a small set of demo users for testing and development.</p>
   *
   * <p>The operation is transactional to ensure the database is
   * properly updated as a single unit of work.</p>
   */
  @Transactional
  public void resetDemoData() {

    // Remove dependent data first
    userRepository.deleteAllUserRoleMappings();
    userRepository.deleteAll();

    bootstrapService.ensureRolesExist();
    bootstrapService.ensureAdminExists();

    createDemoUser("user1", "user123");
    createDemoUser("user2", "user123");
  }

  private void createDemoUser(String username, String password) {
    Role userRole = roleRepository.findByAuthority("USER")
        .orElseThrow(() -> new IllegalStateException("USER role not found."));

    User user = new User(
        null,
        username,
        passwordEncoder.encode(password),
        username + "@example.com",
        LocalDateTime.now(clock),
        Set.of(userRole)
    );

    userRepository.save(user);
  }
}
