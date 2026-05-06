package io.github.ajmiller611.usermanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.dto.UserUpdateRequestDto;
import io.github.ajmiller611.usermanagement.exception.UserAlreadyExistsException;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link UserService} class.
 *
 * <p>This test class verifies the functionality in the {@link UserService},
 * focusing on database interactions, integration with the {@code @CheckUserExistence} aspect,
 * and exception handling.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>User Creation:</strong> Ensures {@code createAndSaveUser()} saves the user details
 *     including encoding the password and the correct roles are assigned.
 *   </li>
 *   <li>
 *     <strong>Aspect Interception for Existing Users:</strong> Confirms that the
 *     {@code @CheckUserExistence} aspect intercepts the call to {@code createAndSaveUser()}
 *     and properly throws a {@link UserAlreadyExistsException} when a user with the
 *     given username already exists in the database.
 *   </li>
 *   <li>
 *     <strong>Aspect Interception for Nonexistent Users:</strong> Confirms that the
 *     {@code @CheckUserExistence} aspect intercepts the call to {@code getUserById()},
 *     {@code updateUser()}, and {@code deleteUser()} throws a {@link UserNotFoundException}
 *     when attempting to retrieve a nonexistent user.
 *   </li>
 *   <li>
 *     <strong>Exception Propagation:</strong> Ensures that the exception thrown by the aspect is
 *     propagated to the {@code createAndSaveUser} method, satisfying the integration requirements.
 *   </li>
 *   <li>
 *     <strong>Database Integrity:</strong> Verifies that the number of users in the database
 *     remains unchanged when the {@code createAndSaveUser} method is invoked with a duplicate user,
 *     ensuring no unintended side effects occur.
 *   </li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Transactional
class UserServiceIntegrationTests {

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  /** Verify {@code createAndSaveUser()} saves user with encoded password and correct roles. */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenUserSavedProperly() {
    UserRequestDto userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    userService.createAndSaveUser(userRequestDto);
    User user = userRepository.findByUsername("testUser").orElseThrow();

    assertNotNull(user, "The user should not be null after saving.");
    assertNotNull(user.getUserId(), "The user ID should not be null.");
    assertEquals(userRequestDto.getUsername(), user.getUsername(),
        "The username should match the provided username.");
    assertTrue(passwordEncoder.matches(userRequestDto.getPassword(), user.getPassword()),
        "The password should be correctly encoded.");
    assertEquals(userRequestDto.getEmail(), user.getEmail(),
        "The email should match the provided email.");
    assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now()),
        "The creation date should be before the current date.");
    assertTrue(user.getAuthorities().contains(roleRepository.findByAuthority("USER").get()),
        "The user should have the 'USER' role assigned.");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code createAndSaveUser()} and throws a {@link UserAlreadyExistsException} when a user already
   * exists. The exception is propagated to {@code createAndSaveUser()} which satisfies the test.
   */
  @Test
  void givenExistingUserWhenCreateAndSaveUserThenThrowUserAlreadyExistsException() {
    String existingUsername = "existingUser";
    User existingUser = new User(
        2L,
        existingUsername,
        "password",
        "existing@example.com",
        LocalDateTime.now(),
        Set.of(roleRepository.findByAuthority("USER").get())
    );
    userRepository.save(existingUser);

    UserRequestDto userRequestDto = new UserRequestDto(
        existingUsername,
        "password",
        "newUser@example.com"
    );

    assertThrows(UserAlreadyExistsException.class,
        () -> userService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an existing user");

    Long userCountAfter = userRepository.count();
    assertEquals(2, userCountAfter,
        "The number of users in the database should not have changed.");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code getUserById()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code getUserById()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenGetUserByIdThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    assertThrows(UserNotFoundException.class,
        () -> userService.getUserById(nonExistentId),
        "Expected getUserById to throw a UserNotFoundException for a nonexistent user");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code updateUser()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code updateUser()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenUpdateUserThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    UserUpdateRequestDto updateRequestDto = new UserUpdateRequestDto(
        "updatedUsername",
        "updatedEmail@example.com"
    );
    assertThrows(UserNotFoundException.class,
        () -> userService.updateUser(nonExistentId, updateRequestDto),
        "Expected updateUser to throw a UserNotFoundException for a nonexistent user");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code deleteUser()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code deleteUser()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenDeleteUserThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    assertThrows(UserNotFoundException.class,
        () -> userService.deleteUser(nonExistentId),
        "Expected deleteUser to throw a UserNotFoundException for a nonexistent user");
  }
}