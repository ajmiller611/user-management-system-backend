package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/**
 * Custom exception thrown when a user is not found in the database.
 */
@Getter
public class UserNotFoundException extends RuntimeException {
  private final String operation;

  /**
   * Constructs a new {@code UserNotFoundException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   * @param operation the context of what operation threw this exception
   */
  public UserNotFoundException(String message, String operation) {
    super(message);
    this.operation = operation;
  }
}
