package io.github.ajmiller611.usermanagement.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.exception.RoleNotFoundException;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.service.UserService;
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
 * Unit tests for the {@link UserService} class.
 *
 * <p>This test class focuses on verifying the core functionality of the
 * {@link UserService}, including the mapping of entities to DTOs, creation and
 * saving of users, role validation, and user authentication.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>DTO Mapping:</strong> Confirms that a {@link User} is accurately mapped
 *     to a {@link UserDto}, including all fields and authorities with the exception for
 *     the password field.
 *   </li>
 *   <li>
 *     <strong>User Creation:</strong> Ensures that a valid {@link UserRequestDto} results in
 *     a {@link UserDto} with correct details, and verifies logging during user creation.
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
class UserServiceCreateTests {

  @InjectMocks private UserService userService;
  @Mock private UserRepository userRepository;
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
  User user;

  /** Sets up test data for use before each test case. */
  @BeforeEach
  void setUp() {
    userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    userRole = new Role("USER");
    user = new User(
        2L,
        userRequestDto.getUsername(),
        userRequestDto.getPassword(),
        userRequestDto.getEmail(),
        fixedTimestamp,
        Set.of(userRole)
    );
  }

  /** Ensure a {@link User} maps properly to a {@link UserDto}. */
  @Test
  void givenUserWhenMapToUserDtoThenReturnUserDto() {
    UserDto dto = userService.mapToUserDto(user);

    assertNotNull(dto, "UserDto should not be null");
    assertEquals(user.getUserId(), dto.getUserId(), "User IDs should match");
    assertEquals(user.getUsername(), dto.getUsername(), "Usernames should match");
    assertEquals(user.getEmail(), dto.getEmail(), "Emails should match");
    assertEquals(user.getCreatedAt(), dto.getCreatedAt(), "Creation timestamps should match");
    assertEquals(1, dto.getAuthorities().size(), "Authority size should be 1");
    assertEquals("USER", dto.getAuthorities().iterator().next().getAuthority(),
        "Authority should be 'USER'");
  }

  /**
   * Verify a valid request DTO returns a {@link UserDto} with the created user's details.
   */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenReturnUserDto() {
    // Mock timestamp with a fixed timestamp to be able to test it properly
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority(userRole.getAuthority())).thenReturn(Optional.of(userRole));
    when(userRepository.save(any(User.class))).thenReturn(user);

    try (LogCaptor logCaptor = LogCaptor.forClass(UserService.class)) {
      UserDto userDto = userService.createAndSaveUser(userRequestDto);

      assertNotNull(userDto, "Created UserDto should not be null");
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
          .withFailMessage("Expected log message for user creation to "
              + "contain 'User created:' along with user details (ID, username, email, "
              + "creation time, authorities)")
          .contains("User created:",
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
        () -> userService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for role not found");
  }

  /** Verify an existing user's {@link UserDetails} is return during an authentication query. */
  @Test
  void givenUserExistsWhenLoadUserByUsernameThenReturnUserDetails() {
    String username = "testUser";
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

    UserDetails userDetails = userService.loadUserByUsername(username);

    assertNotNull(userDetails, "UserDetails should not be null");
    assertEquals(username, userDetails.getUsername(), "Usernames should match");
    verify(userRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify a {@link UsernameNotFoundException} is thrown when a user does not exist during
   * an authentication query.
   */
  @Test
  void givenUserDoesNotExistsWhenLoadUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> userService.loadUserByUsername(username),
        "Expected loadUserByUsername to throw exception when user does not exist.");
    verify(userRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify an existing user's {@link UserDto} is return during a controller's query.
   */
  @Test
  void givenUserExistsWhenGetUserByUsernameThenReturnUserDto() {
    String username = "testUser";
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

    UserDto userDto = userService.getUserByUsername(username);

    assertNotNull(userDto, "UserDto should not be null");
    assertEquals(username, userDto.getUsername(), "Usernames should match");
    verify(userRepository, times(1)).findByUsername(username);
  }

  /**
   * Verify a {@link UsernameNotFoundException} is thrown when a user does not exist during
   * controllers request for user details.
   */
  @Test
  void givenUserDoesNotExistsWhenGetUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> userService.getUserByUsername(username),
        "Expected getUserByUsername to throw exception when user does not exist.");
    verify(userRepository, times(1)).findByUsername(username);
  }
}
