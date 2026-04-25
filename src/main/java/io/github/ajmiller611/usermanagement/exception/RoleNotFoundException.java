package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/** Custom exception thrown when a role is not found in the database. */
@Getter
public class RoleNotFoundException extends RuntimeException {

  /**
   * Constructs a new {@code RoleNotFoundException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public RoleNotFoundException(String message) {
    super(message);
  }

}
