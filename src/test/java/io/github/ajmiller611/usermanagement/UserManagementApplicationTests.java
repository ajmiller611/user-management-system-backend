package io.github.ajmiller611.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
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

  @Mock private RoleRepository roleRepository;
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Environment env;
  @Mock private Clock clock;
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

  /*
   * Tests the behavior when the ADMIN role does not exist in the database.
   * Verifies that an ADMIN role and USER role are added to the database.
   * Verifies that a User with the role of admin is added to the database.
   */
  @Test
  void testRunWhenAdminRoleDoesNotExist() throws Exception {
    // Mock behaviors when the admin role is not present
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.empty());

    // Use a fixed time stamp to be able to have an expected value to test.
    LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
    Clock fixedClock = Clock.fixed(fixedTimestamp
        .atZone(ZoneId.systemDefault())
        .toInstant(), ZoneId.systemDefault());
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());

    Role adminRole = new Role("ADMIN");
    adminRole.setRoleId(1); // Manually set the id in the mocked role instance
    Role userRole = new Role("USER");
    userRole.setRoleId(2); // Manually set the id in the mocked role instance
    when(roleRepository.save(any(Role.class))).thenReturn(adminRole).thenReturn(userRole);

    when(env.getProperty("ADMIN_PASSWORD")).thenReturn("test-password");

    User adminUser = new User(
        1L,
        "admin",
        "encodedPassword",
        "admin@example.com",
        fixedTimestamp,
        Set.of(userRole, adminRole)
    );
    when(userRepository.save(any(User.class))).thenReturn(adminUser);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    // Set up log capturing
    try (LogCaptor logCaptor = LogCaptor.forClass(UserManagementApplication.class)) {

      /*
       * Retrieve the CommandLineRunner Bean, which returns a lambda expression for execution.
       * Getting the Bean directly from the application bypasses Spring's profile-based
       * bean management. This allows testing of the bean in the test profile,
       * even if the bean is normally restricted to the 'dev' profile
       */
      CommandLineRunner commandLineRunner = application.run(
          roleRepository,
          userRepository,
          passwordEncoder,
          clock
      );
      // Invoke the run method of the CommandLineRunner Bean, which executes the lambda expression
      commandLineRunner.run();

      // Verify that Roles were saved with correct values
      ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
      verify(roleRepository, times(2)).save(roleCaptor.capture());
      List<Role> savedRoles = roleCaptor.getAllValues();
      assertThat(savedRoles).extracting(Role::getAuthority)
          .containsExactlyInAnyOrder("ADMIN", "USER");

      // Verify that the Admin User was saved with the correct values
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      assertEquals(1L, userCaptor.getValue().getUserId(),
          "The user ID should be 1 for the admin user.");

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
          .withFailMessage("Logs should indicate the creation of both roles and the admin user.")
          .containsExactly(
              "Role created: Role(roleId=1, authority=ADMIN)",
              "Role created: Role(roleId=2, authority=USER)",
              "Admin User created: " + userCaptor.getValue().toString()
          );
    }
  }

  /*
   * Tests the behavior when the ADMIN role does exist.
   * Verifies that a save of any role does not happen to the database.
   * Verifies that a save of a User does not happen to the database.
   */
  @Test
  void testRunWhenAdminRoleDoesExist() throws Exception {
    // Mock behaviours when Admin role exists
    when(roleRepository.findByAuthority("ADMIN"))
        .thenReturn(Optional.of(new Role("ADMIN")));

    /*
     * Retrieve the CommandLineRunner Bean, which returns a lambda expression for execution.
     * Getting the Bean directly from the application bypasses Spring's profile-based
     * bean management. This allows testing of the bean in the test profile,
     * even if the bean is normally restricted to the 'dev' profile
     */
    CommandLineRunner commandLineRunner = application.run(
        roleRepository,
        userRepository,
        passwordEncoder,
        clock
    );
    // Invoke the run method of the CommandLineRunner Bean, which executes the lambda expression
    commandLineRunner.run();

    // Verify the repositories do not perform any save operations.
    verify(roleRepository, never()).save(any(Role.class));
    verify(userRepository, never()).save(any(User.class));
  }
}
