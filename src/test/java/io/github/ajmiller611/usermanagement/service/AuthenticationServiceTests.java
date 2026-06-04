package io.github.ajmiller611.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.dto.AuthTokensDto;
import io.github.ajmiller611.usermanagement.dto.RegistrationRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserDto;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.exception.InvitationNotFoundException;
import io.github.ajmiller611.usermanagement.model.Invitation;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.security.JwtService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link AuthenticationService} class, verifying user authentication
 * functionality.
 *
 * <p>This test class ensures the correct behavior of authentication methods
 * in the {@link AuthenticationService} by mocking dependencies and simulating different
 * user login scenarios.
 * </p>
 *
 * <p>Test cases include:
 * <ul>
 *   <li><strong>Successful user login:</strong> Verifies that valid credentials produce JWT tokens
 *   and return a populated {@link AuthTokensDto}.</li>
 *   <li><strong>Authentication failure:</strong> Tests that invalid credentials return an empty
 *   {@code AuthTokensDto} with no user information.</li>
 *   <li><strong>Non-existing user login:</strong> Ensures that attempting to log in with
 *   a non-existent username returns an empty {@code AuthTokensDto}.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthenticationServiceTests {

  @InjectMocks private AuthenticationService authenticationService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserService userService;
  @Mock private UserRepository userRepository;
  @Mock private InvitationService invitationService;
  @Mock private JwtService jwtService;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  private UserRequestDto validUserRequest;
  private User user;
  private Authentication auth;
  private UserDto userDto;
  private Map<String, String> tokens;

  /**
   * Sets up test data and mock objects before each test case.
   *
   * <p>This method includes the details for a {@link UserRequestDto}, {@link UserDto},
   * and {@link User} objects. This represents everything needed to mock a user in the
   * system.
   * </p>
   *
   * <p>A {@link UsernamePasswordAuthenticationToken} is set up to simulate user credentials
   * for authentication purposes. As well as a map of token strings (access and refresh tokens)
   * to simulate a successful login response.</p>
   */
  @BeforeEach
  void setUp() {
    validUserRequest = new UserRequestDto("validUser", "validPassword", "valid@email.com");

    Role userRole = new Role("ROLE_USER");
    user = new User(
        2L,
        "validUser",
        "validPassword",
        "valid@email.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    userDto = new UserDto(
        2L,
        "validUser",
        "valid@email.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    auth = new UsernamePasswordAuthenticationToken("validUser", "validPassword");

    tokens = Map.of("accessToken", "validAccessToken", "refreshToken", "validRefreshToken");
  }

  /**
   * Test case for when valid credentials passed to the service. The service should return with a
   * {@link AuthTokensDto} object containing access and refresh JWT tokens as well as a
   * {@link UserDto} containing the user's details.
   */
  @Test
  void givenValidCredentialsWhenLoginUserThenReturnAuthTokensDto() {
    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
    when(jwtService.generateTokens(auth)).thenReturn(tokens);
    when(userRepository.findByUsername("validUser"))
        .thenReturn(Optional.of(user));
    when(userService.mapToUserDto(user)).thenReturn(userDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(AuthenticationService.class)) {
      AuthTokensDto result = authenticationService.loginUser(validUserRequest);

      assertEquals("validAccessToken", result.getAccessToken(),
          "The access token in the response should match the expected valid access token.");

      assertEquals("validRefreshToken", result.getRefreshToken(),
          "The refresh token in the response should match the expected valid refresh token.");

      assertEquals(userDto, result.getUserDto(),
          "The user DTO in the response should match the mapped user DTO.");

      assertThat(logCaptor.getInfoLogs().getFirst())
          .withFailMessage("The log should indicate that a login request was received "
              + "with the correct username.")
          .contains("Login request received with username: validUser");

      assertThat(logCaptor.getInfoLogs().get(1))
          .withFailMessage("The log should confirm successful authentication for the user.")
          .contains("Authentication successful for validUser");

      assertThat(logCaptor.getInfoLogs().get(2))
          .withFailMessage("The log should confirm that the correct user details were returned.")
          .contains(String.format("User details returned: %s", userDto));
    }
  }

  /**
   * Test case for when an invalid credentials is provided to the service. The service should return
   * an empty {@link AuthTokensDto} due to authentication failure.
   */
  @Test
  void givenInvalidCredentialsWhenLoginUserThenReturnEmptyAutoTokensDto() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new AuthenticationException("Invalid credentials") {});

    AuthTokensDto result = authenticationService.loginUser(validUserRequest);

    assertEquals("", result.getAccessToken(),
        "The access token should be empty when authentication fails due to "
            + "invalid credentials.");

    assertEquals("", result.getRefreshToken(),
        "The refresh token should be empty when authentication fails due to "
            + "invalid credentials.");

    assertNull(result.getUserDto(),
        "The user DTO should be null when authentication fails due to "
            + "invalid credentials.");
  }

  /**
   * Test case for when a non-existent username is provided to the server. The service should return
   * an empty {@link AuthTokensDto} due to the user not existing in the database.
   */
  @Test
  void givenNonExistingUserWhenLoginUserThenReturnEmptyAutoTokensDto() {
    validUserRequest.setUsername("NonExistingUser");

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
    when(userRepository.findByUsername("NonExistingUser")).thenReturn(Optional.empty());

    AuthTokensDto result = authenticationService.loginUser(validUserRequest);

    assertEquals("", result.getAccessToken(),
        "The access token should be empty when the user does not exist in the database.");

    assertEquals("", result.getRefreshToken(),
        "The refresh token should be empty when the user does not exist in the database.");

    assertNull(result.getUserDto(),
        "The user DTO should be null when the user does not exist in the database.");
  }

  /**
   * Verifies a valid request dto creates a user and returns a {@link UserDto} with the
   * created user's details.
   */
  @Test
  void givenValidRequestDtoWhenCompleteRegistrationThenReturnUserDto() {
    Role role  = new Role("USER");
    LocalDateTime createdAtTimestamp = LocalDateTime.of(2026, 5, 1, 0, 0, 0, 0);
    LocalDateTime expiresAtTimestamp = LocalDateTime.of(2026, 5, 8, 0, 0, 0, 0);
    Invitation invitation = new Invitation(
        1L,
        "test@email.com",
        "token",
        role,
        createdAtTimestamp,
        expiresAtTimestamp,
        false
    );

    RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto(
        invitation.getToken(),
        "testUser",
        "password"
    );

    UserRequestDto userRequestDto = new UserRequestDto(
        registrationRequestDto.getUsername(),
        registrationRequestDto.getPassword(),
        invitation.getEmail()
    );

    userDto = new UserDto(
        2L,
        userRequestDto.getEmail(),
        userRequestDto.getEmail(),
        fixedTimestamp,
        Set.of(role)
    );

    when(invitationService.getValidInvitation(any(String.class))).thenReturn(invitation);
    when(userService.createAndSaveUser(any(UserRequestDto.class))).thenReturn(this.userDto);

    UserDto result = authenticationService.completeRegistration(registrationRequestDto);

    assertNotNull(result, "UserDto should not be null");
    assertEquals(userDto.getUserId(), result.getUserId(),
        "UserDto's id should be the same");
    assertEquals(userDto.getUsername(), result.getUsername(),
        "UserDto's username should be the same");
    assertEquals(userDto.getEmail(), result.getEmail(),
        "UserDto's email should be the same");
    assertEquals(userDto.getCreatedAt(), result.getCreatedAt(),
        "UserDto's createdAt should be the same");
    assertEquals(userDto.getAuthorities(), result.getAuthorities(),
        "UserDto's authorities should be the same");

    verify(invitationService, times(1)).getValidInvitation(invitation.getToken());
    verify(userService, times(1)).createAndSaveUser(any(UserRequestDto.class));
    verify(invitationService, times(1)).markInvitationAsUsed(invitation);
  }

  /** Verifies invitation validation exceptions are propagated and registration is aborted. */
  @Test
  void givenNonExistingInvitationWhenCompleteRegistrationThenReturnException() {
    RegistrationRequestDto registrationRequestDto = new RegistrationRequestDto(
        "nonExistingToken",
        "testUser",
        "password"
    );
    when(invitationService.getValidInvitation(any(String.class)))
        .thenThrow(new InvitationNotFoundException("Invitation not found"));

    assertThrows(InvitationNotFoundException.class,
        () -> authenticationService.completeRegistration(registrationRequestDto));

    verify(userService, never()).createAndSaveUser(any(UserRequestDto.class));
    verify(invitationService, never()).markInvitationAsUsed(any(Invitation.class));
  }
}
