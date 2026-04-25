package io.github.ajmiller611.usermanagement.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.exception.UnauthorizedOperationException;
import io.github.ajmiller611.usermanagement.model.Role;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.service.UserService;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link UserService} class.
 *
 * <p>This test class validates the {@code deleteUser} method of the {@code UserService},
 * focusing on its ability to handle various deletion scenarios, enforce business rules, and manage
 * errors effectively.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Successful User Deletion:</strong> Verifies that a valid user ID successfully deletes
 *     the corresponding user and interacts with the repository as expected.
 *   </li>
 *   <li>
 *     <strong>Protection Against Unauthorized Operations:</strong> Confirms that attempting to
 *     delete a user with the "ADMIN" role results in an {@link UnauthorizedOperationException},
 *     preventing deletion.
 *   </li>
 *   <li>
 *     <strong>Repository Interaction:</strong> Ensures that the method correctly interacts with
 *     mocked {@link UserRepository} by verifying the invocation of the appropriate methods
 *     under various scenarios.
 *   </li>
 *   <li>
 *     <strong>Exception Messages:</strong> Verifies that exceptions thrown include informative
 *     and user-friendly messages for easier debugging and error reporting.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceDeleteTests {

  @InjectMocks private UserService userService;
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  private static final Long VALID_USER_ID = 2L;
  User testUser;

  /**
   * Set up a new test user object for use in each test.
   */
  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUserId(VALID_USER_ID);
    testUser.setAuthorities(Set.of(new Role("USER")));
  }

  /**
   * Verifies that a valid user id will delete the user of that id.
   */
  @Test
  void givenValidUserIdWhenDeleteUserThenUserIsDeleted() {
    when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

    userService.deleteUser(VALID_USER_ID);

    verify(userRepository, times(1)).findById(VALID_USER_ID);
    verify(userRepository, times(1)).deleteById(VALID_USER_ID);
  }

  /**
   * Verifies that a user id of an admin user throws an {@link UnauthorizedOperationException}.
   */
  @Test
  void givenAdminUserIdWhenDeleteUserThenThrowUnauthorizedOperationException() {
    testUser.setAuthorities(Set.of(new Role("ADMIN")));
    when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> userService.deleteUser(VALID_USER_ID));

    assertNotNull(exception, "Exception must not be null");
    assertEquals("Unauthorized user cannot delete admin user with id 2", exception.getMessage(),
        "Expected exception message to be 'Unauthorized user cannot delete admin user with id 2'");
    verify(userRepository, never()).deleteById(any(Long.class));
  }
}
