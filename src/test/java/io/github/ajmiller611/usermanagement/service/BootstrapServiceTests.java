package io.github.ajmiller611.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link BootstrapService}.
 *
 * <p>This test suite verifies system initialization logic responsible for:
 * <ul>
 *   <li>Ensuring required roles ("ADMIN", "USER") exist</li>
 *   <li>Creating missing roles when necessary</li>
 *   <li>Ensuring a default admin user exists</li>
 *   <li>Validating correct behavior when data already exists</li>
 * </ul>
 *
 * <p>Tests use Mockito to mock repository and external dependencies,
 * and verify behavior through interactions and captured arguments.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class BootstrapServiceTests {

  @Mock private RoleRepository roleRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Environment env;
  @Mock private Clock clock;
  @InjectMocks private BootstrapService bootstrapService;

  /**
   * Verifies that no roles are created when both ADMIN and USER roles already exist.
   */
  @Test
  void testEnsureRolesExistWhenRolesExist() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(new Role("USER")));

    bootstrapService.ensureRolesExist();

    verify(roleRepository, times(1)).findByAuthority("ADMIN");
    verify(roleRepository, times(1)).findByAuthority("USER");

    verify(roleRepository, never()).save(any(Role.class));
  }

  /**
   * Verifies that the ADMIN role is created when it does not exist,
   * while existing roles remain unchanged.
   */
  @Test
  void testEnsureRolesExistWhenAdminRoleDoesNotExist() {
    // Mock behaviors when the admin role is not present
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.empty());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(new Role("USER")));

    Role adminRole = new Role("ADMIN");

    when(roleRepository.save(any(Role.class))).thenReturn(adminRole);

    // Set up log capturing
    try (LogCaptor logCaptor = LogCaptor.forClass(BootstrapService.class)) {
      bootstrapService.ensureRolesExist();

      // Verify that the ADMIN role was saved
      ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
      verify(roleRepository, times(1)).save(roleCaptor.capture());
      Role savedRole = roleCaptor.getValue();
      assertThat(savedRole.getAuthority()).isEqualTo("ADMIN");

      // Verify that logging happened correctly
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Logs should indicate the creation of the ADMIN role.")
          .anyMatch(log -> log.contains("Role created"));
    }
  }

  /**
   * Verifies that the USER role is created when it does not exist,
   * while existing roles remain unchanged.
   */
  @Test
  void testEnsureRolesExistWhenUserRoleDoesNotExist() {
    // Mock behaviors when the user role is not present
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.empty());

    Role userRole = new Role("USER");

    when(roleRepository.save(any(Role.class))).thenReturn(userRole);

    // Set up log capturing
    try (LogCaptor logCaptor = LogCaptor.forClass(BootstrapService.class)) {
      bootstrapService.ensureRolesExist();

      // Verify that the USER role was saved
      ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
      verify(roleRepository, times(1)).save(roleCaptor.capture());
      Role savedRole = roleCaptor.getValue();
      assertThat(savedRole.getAuthority()).isEqualTo("USER");

      // Verify that logging happened correctly
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Logs should indicate the creation of the USER role")
          .anyMatch(log -> log.contains("Role created"));
    }
  }

  /**
   * Verifies that no admin user is created when one already exists.
   *
   * <p>Ensures early return prevents unnecessary repository and service calls.</p>
   */
  @Test
  void testEnsureAdminExistWhenAdminExist() {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

    bootstrapService.ensureAdminExists();

    verify(userRepository).findByUsername("admin");
    verify(userRepository, never()).save(any());

    verifyNoInteractions(roleRepository);
    verifyNoInteractions(passwordEncoder, env);
  }

  /**
   * Verifies full admin creation when no admin user exists.
   *
   * <p>Ensures:
   * <ul>
   *   <li>Roles are loaded from repository</li>
   *   <li>Password is encoded using PasswordEncoder</li>
   *   <li>Environment variable is used for admin password</li>
   *   <li>User entity is created with correct fields and persisted</li>
   * </ul>
   * </p>
   */
  @Test
  void testEnsureAdminExistWhenAdminDoesNotExist() {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(new Role("USER")));

    when(env.getProperty("ADMIN_PASSWORD")).thenReturn("test-password");

    // Use a fixed time stamp to be able to have an expected value to test.
    LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
    Clock fixedClock = Clock.fixed(fixedTimestamp
        .atZone(ZoneId.systemDefault())
        .toInstant(), ZoneId.systemDefault());
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());

    Role adminRole = new Role("ADMIN");
    Role userRole = new Role("USER");

    User adminUser = new User(
        null,
        "admin",
        "encodedPassword",
        "admin@example.com",
        fixedTimestamp,
        Set.of(userRole, adminRole)
    );

    when(userRepository.save(any(User.class))).thenReturn(adminUser);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    // Set up log capturing
    try (LogCaptor logCaptor = LogCaptor.forClass(BootstrapService.class)) {
      bootstrapService.ensureAdminExists();

      // Verify that the Admin User was saved with the correct values
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      assertNull(userCaptor.getValue().getUserId(),
          "The user ID should be null for the admin user.");

      assertEquals("admin", userCaptor.getValue().getUsername(),
          "The username for the admin user should be 'admin'.");

      assertEquals("encodedPassword", userCaptor.getValue().getPassword(),
          "The password should be encoded as 'encodedPassword'.");

      assertEquals("admin@example.com", userCaptor.getValue().getEmail(),
          "The email address for the admin user should be 'admin@example.com'.");

      assertEquals(fixedTimestamp, userCaptor.getValue().getCreatedAt(),
          "The creation timestamp for the admin user should match the fixed timestamp.");

      assertEquals(Set.of(userRole, adminRole), userCaptor.getValue().getAuthorities(),
          "The admin user should have both 'USER' and 'ADMIN' roles.");

      // Verify that logging happened correctly
      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Logs should indicate the creation of the admin user.")
          .anyMatch(log -> log.contains("Admin created"));
    }
  }
}
