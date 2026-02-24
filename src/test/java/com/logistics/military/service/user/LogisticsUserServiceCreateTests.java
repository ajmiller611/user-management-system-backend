package com.logistics.military.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.exception.RoleNotFoundException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class.
 *
 * <p>This test class focuses on verifying the core functionality of the
 * {@link LogisticsUserService}, including the mapping of entities to DTOs, creation and
 * saving of users, role validation, and user authentication.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>DTO Mapping:</strong> Confirms that a {@link LogisticsUser} is accurately mapped
 *     to a {@link LogisticsUserDto}, including all fields and authorities with the exception for
 *     the password field.
 *   </li>
 *   <li>
 *     <strong>User Creation:</strong> Ensures that a valid {@link UserRequestDto} results in
 *     a {@link LogisticsUserDto} with correct details, and verifies logging during user creation.
 *   </li>
 *   <li>
 *     <strong>Role Validation:</strong> Validates that a {@link Role} must exist for a user to
 *     be created. Tests for proper handling of missing roles, including throwing an
 *     {@link RoleNotFoundException}.
 *   </li>
 *   <li>
 *     <strong>User Authentication:</strong> Confirms that {@code loadUserByUsername} returns
 *     the correct {@link UserDetails} for an existing user and throws a
 *     {@link UsernameNotFoundException} for non-existent users.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceCreateTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  UserRequestDto userRequestDto;
  Role userRole;
  LogisticsUser user;

  /** Sets up test data for use before each test case. */
  @BeforeEach
  void setUp() {
    userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    userRole = new Role("USER");
    user = new LogisticsUser(
        2L,
        userRequestDto.getUsername(),
        userRequestDto.getPassword(),
        userRequestDto.getEmail(),
        fixedTimestamp,
        Set.of(userRole)
    );
  }

  /** Ensure a {@link LogisticsUser} maps properly to a {@link LogisticsUserDto}. */
  @Test
  void givenLogisticsUserWhenMapToUserDtoThenReturnLogisticsUserDto() {
    LogisticsUserDto dto = logisticsUserService.mapToUserDto(user);

    assertNotNull(dto, "LogisticsUserDto should not be null");
    assertEquals(user.getUserId(), dto.getUserId(), "User IDs should match");
    assertEquals(user.getUsername(), dto.getUsername(), "Usernames should match");
    assertEquals(user.getEmail(), dto.getEmail(), "Emails should match");
    assertEquals(user.getCreatedAt(), dto.getCreatedAt(), "Creation timestamps should match");
    assertEquals(1, dto.getAuthorities().size(), "Authority size should be 1");
    assertEquals("USER", dto.getAuthorities().iterator().next().getAuthority(),
        "Authority should be 'USER'");
  }

  /**
   * Verify a valid request DTO returns a {@link LogisticsUserDto} with the created user's details.
   */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenReturnLogisticsUserDto() {
    // Mock timestamp with a fixed timestamp to be able to test it properly
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority(userRole.getAuthority())).thenReturn(Optional.of(userRole));
    when(logisticsUserRepository.save(any(LogisticsUser.class))).thenReturn(user);

    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserService.class)) {
      LogisticsUserDto userDto = logisticsUserService.createAndSaveUser(userRequestDto);

      assertNotNull(userDto, "Created LogisticsUserDto should not be null");
      assertEquals(user.getUserId(), userDto.getUserId(), "User IDs should match");
      assertEquals(user.getUsername(), userDto.getUsername(), "Usernames should match");
      assertEquals(user.getEmail(), userDto.getEmail(), "Emails should match");
      assertEquals(user.getCreatedAt(), userDto.getCreatedAt(), "Creation timestamps should match");
      assertEquals(user.getAuthorities(), userDto.getAuthorities(), "Authorities should match");

      assertThat(logCaptor.getInfoLogs().getFirst())
          .withFailMessage("Expected log message for create user request to "
              + "contain 'Create user request with DTO:' along with the username and email")
          .contains("Create user request with DTO:", user.getUsername(), user.getEmail());

      assertThat(logCaptor.getInfoLogs().get(1))
          .withFailMessage("Expected log message for logistics user creation to "
              + "contain 'LogisticsUser created:' along with user details (ID, username, email, "
              + "creation time, authorities)")
          .contains("LogisticsUser created:",
              user.getUserId().toString(),
              user.getUsername(),
              user.getEmail(),
              user.getCreatedAt().toString(),
              user.getAuthorities().toString());

    }
  }

  /** Verify an {@link RoleNotFoundException} is thrown when the 'USER' role does not exist. */
  @Test
  void givenRoleNotFoundWhenCreateAndSaveUserThenThrowRoleNotFoundException() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.empty());

    assertThrows(RoleNotFoundException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for role not found");
  }

  /** Verify an existing user's {@link UserDetails} is return during an authentication query. */
  @Test
  void givenUserExistsWhenLoadUserByUsernameThenReturnUserDetails() {
    String username = "testUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.of(user));

    UserDetails userDetails = logisticsUserService.loadUserByUsername(username);

    assertNotNull(userDetails, "UserDetails should not be null");
    assertEquals(username, userDetails.getUsername(), "Usernames should match");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify a {@link UsernameNotFoundException} is thrown when a user does not exist during
   * an authentication query.
   */
  @Test
  void givenUserDoesNotExistsWhenLoadUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> logisticsUserService.loadUserByUsername(username),
        "Expected loadUserByUsername to throw exception when user does not exist.");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify an existing user's {@link LogisticsUserDto} is return during a controller's query.
   */
  @Test
  void givenUserExistsWhenGetUserByUsernameThenReturnLogisticsUserDto() {
    String username = "testUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.of(user));

    LogisticsUserDto userDto = logisticsUserService.getUserByUsername(username);

    assertNotNull(userDto, "LogisticUserDto should not be null");
    assertEquals(username, userDto.getUsername(), "Usernames should match");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify a {@link UsernameNotFoundException} is thrown when a user does not exist during
   * controllers request for user details.
   */
  @Test
  void givenUserDoesNotExistsWhenGetUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> logisticsUserService.getUserByUsername(username),
        "Expected getUserByUsername to throw exception when user does not exist.");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }
}
