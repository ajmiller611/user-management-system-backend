package io.github.ajmiller611.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link DemoResetService}, validating the behavior of the
 * demo reset functionality.
 *
 * <p>This test ensures that the reset process:
 * <ul>
 *   <li>Clears existing user data and user-role mappings</li>
 *   <li>Delegates role and admin restoration to {@code BootstrapService}</li>
 *   <li>Creates expected demo users with correctly mapped data</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DemoResetServiceTests {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;
  @Mock private BootstrapService bootstrapService;

  @InjectMocks private DemoResetService demoResetService;

  /**
   * Verifies that resetting demo data behaves correctly.
   * <ul>
   *   <li>Deletes existing user and user-role data</li>
   *   <li>Calls bootstrap service to restore required system state</li>
   *   <li>Creates the expected demo users with correct usernames</li>
   * </ul>
   *
   * <p>Also validates that user creation logic correctly encodes passwords
   * and assigns timestamps using the system clock.</p>
   */
  @Test
  void givenResetDemoData_whenResetDemoData_thenResetDemoData() {
    Role userRole = new Role("USER");

    when(roleRepository.findByAuthority("USER"))
        .thenReturn(Optional.of(userRole));

    when(passwordEncoder.encode(any()))
        .thenReturn("encodedPassword");

    when(clock.instant()).thenReturn(Instant.now());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    demoResetService.resetDemoData();

    verify(userRepository).deleteAllUserRoleMappings();
    verify(userRepository).deleteAll();

    verify(bootstrapService).ensureRolesExist();
    verify(bootstrapService).ensureAdminExists();

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(2)).save(userCaptor.capture());

    assertThat(userCaptor.getAllValues())
        .extracting(User::getUsername)
        .containsExactlyInAnyOrder("testUser1", "testUser2");
  }
}
