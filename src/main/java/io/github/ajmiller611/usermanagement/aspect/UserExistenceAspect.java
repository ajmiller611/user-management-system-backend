package io.github.ajmiller611.usermanagement.aspect;

import io.github.ajmiller611.usermanagement.annotation.CheckUserExistence;
import io.github.ajmiller611.usermanagement.dto.UserRequestDto;
import io.github.ajmiller611.usermanagement.exception.UserAlreadyExistsException;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to validate the existence of a user based on the conditions specified in the
 * {@link CheckUserExistence} annotation.
 *
 * <p>This class intercepts methods annotated with {@link CheckUserExistence} and performs a check
 * to ensure that the user exists, either by username or ID, before the method proceeds.
 * </p>
 *
 * <ul>
 *   <li>If the check is by "username" and the user already exists,
 *   a {@link UserAlreadyExistsException} is thrown.</li>
 *   <li>If the check is by "id" and the user does not exist, a {@link UserNotFoundException}
 *   is thrown.</li>
 * </ul>
 *
 * <p>This aspect supports user creation (where existing users should not be allowed) and
 * user updates or deletions (where non-existing users should not be allowed).
 * </p>
 *
 * <p>The class leverages Spring AOP to manage the interception and validation logic.
 * </p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class UserExistenceAspect {

  private final UserRepository userRepository;

  /**
   * This method validates the existence of a user based on the specified condition defined in the
   * {@link CheckUserExistence} annotation. It intercepts the annotated method, checks whether
   * the user exists by either username or ID, and throws appropriate exceptions.
   *
   * @param joinPoint The {@link ProceedingJoinPoint} that provides reflective access to the method
   *                  being executed, allowing the aspect to control the method invocation.
   * @param checkUserExistence The {@link CheckUserExistence} annotation containing the
   *                           configuration for the check (either by "username" or "id").
   * @return Proceeds with the original method invocation if the validation logic passes;
   *         otherwise, an exception is thrown if the validation fails.
   * @throws Throwable If there is an issue executing the method or if the validation logic
   *         determines either {@link UserAlreadyExistsException} or {@link UserNotFoundException}
   *         exception needs to be thrown.
   */
  @Around("@annotation(checkUserExistence)")
  public Object validateUserExistence(
      ProceedingJoinPoint joinPoint,
      CheckUserExistence checkUserExistence
  ) throws Throwable {

    String checkBy = checkUserExistence.checkBy();
    Object[] args = joinPoint.getArgs();

    // During creation, user existing should cause an exception.
    if ("username".equals(checkBy) && args[0] instanceof UserRequestDto userRequestDto) {
      String username = userRequestDto.getUsername();
      if (userRepository.findByUsername(username).isPresent()) {
        throw new UserAlreadyExistsException(
            String.format("User with username '%s' already exists", username)
        );
      }

    // During update or delete, user not existing should cause an exception.
    } else if ("id".equals(checkBy)
        && args[0] instanceof Long id
        && !userRepository.existsById(id)) {
      throw new UserNotFoundException(
          String.format("User ID '%d' does not exist", id), joinPoint.getSignature().getName());
    }

    return joinPoint.proceed();  // Proceed with the original method call
  }
}
