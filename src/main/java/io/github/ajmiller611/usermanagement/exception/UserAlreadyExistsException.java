package io.github.ajmiller611.usermanagement.exception;

/**
 * Custom exception thrown when attempting to create a user that already exists in the database.
 */
public class UserAlreadyExistsException extends RuntimeException {

  /**
   * Constructs a new {@code UserAlreadyExistsException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   */
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
