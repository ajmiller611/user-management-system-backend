package io.github.ajmiller611.usermanagement.validator;

import io.github.ajmiller611.usermanagement.annotation.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * Validator class for the {@link ValidEmail} annotation.
 * This class provides the validation logic to check whether an email address is valid.
 * It implements the {@link ConstraintValidator} interface, which is used to perform
 * the custom validation.
 *
 * <p>Validation checks include null, empty, missing '@', missing domain extension,
 * and compares each part of the email to a regular expression to valid each part.
 * </p>
 */
public class ValidEmailValidator
    implements ConstraintValidator<ValidEmail, String> {

  /**
   * Validates the provided email to check if the email is valid.
   *
   * @param value The email address to be validated.
   * @param context The context in which the validation is being performed.
   * @return true if the email is valid, false otherwise.
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null || value.trim().isEmpty()) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Email is required")
          .addConstraintViolation();
      return false;
    }

    if (!value.contains("@")) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Email invalid. Missing '@' symbol")
          .addConstraintViolation();
      return false;
    }

    String[] parts = value.split("@");
    String username = parts[0];
    String usernamePattern = "^[A-Za-z0-9+_.-]+$";
    if (!username.matches(usernamePattern)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Username of Email is invalid."
              + " Only letters, digits, '+', '_', '.', and '-' are valid.")
          .addConstraintViolation();
      return false;
    }

    String fullDomain = parts[1];
    if (!fullDomain.contains(".")) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Domain extension is missing (no period).")
          .addConstraintViolation();
      return false;
    }

    String[] domainParts = fullDomain.split("\\.");
    if (domainParts.length < 2) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Domain extension is missing.")
          .addConstraintViolation();
      return false;
    }

    String topLevelDomain = domainParts[domainParts.length - 1];
    // Remove the top level domain from the array
    domainParts = Arrays.copyOfRange(domainParts, 0, domainParts.length - 1);

    String domainPattern = "^[A-Za-z0-9.-]+$";
    for (String part : domainParts) {
      // Empty means consecutive periods in the email which is valid
      if (part.isEmpty()) {
        continue;
      }

      if (!part.matches(domainPattern)) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Domain of Email is invalid."
            + " Only letters, digits, '.', and '-' are valid.")
            .addConstraintViolation();
        return false;
      }
    }

    String topLevelDomainPattern = "^[A-Za-z]{2,}$";
    if (!topLevelDomain.matches(topLevelDomainPattern)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Domain extension is invalid."
              + " Only letters are valid and must be at least 2 characters.")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
