package io.github.ajmiller611.usermanagement.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to specify user existence validation criteria before method execution.
 *
 * <p>This annotation can be applied to methods where user existence needs to be validated either by
 * username or ID. The validation is performed before the method is executed, and an exception is
 * thrown if the validation criteria fails.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckUserExistence {

  /**
   * Specifies the criterion for user existence validation.
   * The value can either be "id" (default) or "username".
   *
   * @return The validation criterion for checking user existence.
   */
  String checkBy() default "id";
}
