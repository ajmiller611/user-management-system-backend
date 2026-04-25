package io.github.ajmiller611.usermanagement.aspect;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.annotation.CheckUserExistence;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.exception.UserAlreadyExistsException;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.model.User;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link UserExistenceAspect} class.
 *
 * <p>This test class verifies the behavior of the {@link UserExistenceAspect},
 * focusing on user existence validation based on specified conditions such as
 * username or ID. It ensures that the aspect correctly intercepts methods annotated
 * with {@code @CheckUserExistence} and enforces the required business rules.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Validation for Existing Users:</strong> Confirms that the aspect intercepts
 *     calls with a username argument and properly throws a {@link UserAlreadyExistsException}
 *     when a user with the given username already exists in the repository.
 *   </li>
 *   <li>
 *     <strong>Validation for Nonexistent Users:</strong> Confirms that the aspect intercepts
 *     calls with an ID argument and throws a {@link UserNotFoundException} when attempting to
 *     reference a nonexistent user in the repository.
 *   </li>
 *   <li>
 *     <strong>Method Execution Continuation:</strong> Ensures that the original method is executed
 *     when all validation checks pass, confirming the aspect does not interfere with valid
 *     operations.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class UserExistenceAspectTests {

  @Mock private UserRepository userRepository;
  @Mock private ProceedingJoinPoint joinPoint;

  private UserExistenceAspect userExistenceAspect;

  @BeforeEach
  void setUp() {
    userExistenceAspect = new UserExistenceAspect((userRepository));
  }

  /** Verify when 'username' argument then an existent user causes an exception to be thrown. */
  @Test
  void givenExistentUserByUsernameWhenValidateUserExistenceThenThrowUserAlreadyExistsException() {
    CheckUserExistence annotation = mock(CheckUserExistence.class);
    UserRequestDto userRequestDto = new UserRequestDto(
        "existingUser",
        "password",
        "test@example.com"
    );

    when(annotation.checkBy()).thenReturn("username");
    when(joinPoint.getArgs()).thenReturn(new Object[] {userRequestDto});
    when(userRepository.findByUsername("existingUser"))
        .thenReturn(Optional.of(new User()));

    assertThrows(UserAlreadyExistsException.class,
        () -> userExistenceAspect.validateUserExistence(joinPoint, annotation),
        "Expected UserAlreadyExistsException for an existing username");
  }

  /** Verify when default 'id' argument then a nonexistent user causes an exception to be thrown. */
  @Test
  void givenNonExistentUserByIdWhenValidateUserExistenceThenThrowUserAlreadyExistsException() {
    CheckUserExistence annotation = mock(CheckUserExistence.class);

    Long nonExistentId = 999L;
    when(annotation.checkBy()).thenReturn("id");
    when(joinPoint.getArgs()).thenReturn(new Object[] {nonExistentId});
    when(userRepository.existsById(nonExistentId)).thenReturn(false);

    Signature mockSignature = mock(Signature.class);
    when(joinPoint.getSignature()).thenReturn(mockSignature);
    when(mockSignature.getName()).thenReturn("validateUserExistence");

    assertThrows(UserNotFoundException.class,
        () -> userExistenceAspect.validateUserExistence(joinPoint, annotation),
        "Expected UserNotFoundException for a nonexistent user ID");
  }

  /** Verify when checks pass then the original method proceeds. */
  @Test
  void givenValidationPassesWhenValidateUserExistenceThenProceedWithOriginalMethod()
      throws Throwable {
    CheckUserExistence annotation = mock(CheckUserExistence.class);

    UserRequestDto userRequestDto = new UserRequestDto(
        "nonExistentUser",
        "password",
        "test@example.com"
    );
    when(annotation.checkBy()).thenReturn("username");
    when(joinPoint.getArgs()).thenReturn(new Object[] {userRequestDto});
    when(userRepository.findByUsername("nonExistentUser"))
        .thenReturn(Optional.empty());

    userExistenceAspect.validateUserExistence(joinPoint, annotation);

    verify(joinPoint, times(1)).proceed();
  }
}
