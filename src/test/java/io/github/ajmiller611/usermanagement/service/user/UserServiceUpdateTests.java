package io.github.ajmiller611.usermanagement.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.dto.UserUpdateRequestDto;
import io.github.ajmiller611.usermanagement.exception.UnauthorizedOperationException;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link UserService} class.
 *
 * <p>This test class verifies the functionality of the {@code updateUser} method, focusing on
 * successful operations, edge cases, and error handling.</p>
 *
 * <h2>Test Scenarios</h2>
 * <ul>
 *   <li>
 *     <strong>Successful Update:</strong> Ensures that a user's details are updated successfully
 *     when valid input is provided.
 *   </li>
 *   <li>
 *     <strong>Unauthorized Operation:</strong> Verifies that attempts to update an admin user
 *     result in an unauthorized operation error.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceUpdateTests {

  @InjectMocks private UserService userService;
  @Mock private UserRepository userRepository;

  UserUpdateRequestDto updateRequestDto;
  Long userId;
  User updatedUser;

  /** Initializes test data before each test case. */
  @BeforeEach
  void setUp() {
    updateRequestDto = new UserUpdateRequestDto(
        "updatedUsername",
        "updatedEmail@example.com"
    );

    userId = 2L;

    updatedUser = new User(
        userId,
        "updatedUsername",
        "password",
        "updatedEmail@example.com",
        LocalDateTime.now(),
        Set.of(new Role("USER"))
    );
  }

  /** Verifies that updating a valid user returns the updated details. */
  @Test
  void givenValidUserUpdateRequestDtoWhenUpdateUserThenUpdatedUser() {
    User user = new User(
        userId,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(new Role("USER"))
    );
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(updatedUser);

    UserResponseDto result = userService.updateUser(userId, updateRequestDto);

    verify(userRepository, times(1)).findById(userId);
    assertNotNull(result, "The result should not be null when a valid user update is performed.");

    assertEquals(userId, result.getUserId(),
        "The user ID in the response should match the input user ID.");

    assertEquals(updateRequestDto.getUsername(), result.getUsername(),
        "The updated username in the response should match the input username.");

    assertEquals(updateRequestDto.getEmail(), result.getEmail(),
        "The updated email in the response should match the input email.");
  }

  /** Verifies that updating an admin user throws {@link UnauthorizedOperationException}. */
  @Test
  void givenAdminUserIdWhenUpdateUserThenThrowsUnauthorizedOperationException() {
    Long adminUserId = 1L;
    User adminUser = mock(User.class);
    when(adminUser.hasRole("ADMIN")).thenReturn(true);

    when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> userService.updateUser(adminUserId, updateRequestDto));

    verify(userRepository, times(1)).findById(adminUserId);
    verify(userRepository, never()).save(any(User.class));
    assertNotNull(exception, "An UnauthorizedOperationException should be thrown when "
        + "attempting to update an admin user.");

    assertEquals(
        String.format("Unauthorized user cannot update admin user with id %d", adminUserId),
        exception.getMessage(),
        "The exception message should indicate the unauthorized attempt to update an "
            + "admin user.");

    assertEquals(adminUserId, exception.getId(), "The exception should reference the "
        + "correct admin user ID.");
  }
}
