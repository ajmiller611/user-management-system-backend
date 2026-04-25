package io.github.ajmiller611.usermanagement.annotation;

import io.github.ajmiller611.usermanagement.validator.ValidEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to validate the email domain extension.
 * This annotation ensures that an email address contains a valid domain extension
 * (e.g., .com, .org, .net).
 *
 * <p>The validation logic is provided by the {@link ValidEmailValidator} class.</p>
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEmailValidator.class)
public @interface ValidEmail {

  /**
   * The error message to be returned when the email does not contain a valid domain extension.
   *
   * @return the error message
   */
  String message() default "Email should contain a valid domain extension.";

  /**
   * The groups to which this constraint belongs.
   *
   * @return the validation groups
   */
  Class<?>[] groups() default {};

  /**
   * Carries additional metadata about the constraint which can be used to pass custom data
   * to the validator.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};
}

