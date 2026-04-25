package io.github.ajmiller611.usermanagement.exception;

/**
 * Custom exception thrown when an error occurs during the saving to database process.
 */
public class UserCreationException extends RuntimeException {

  /**
   * Constructs a new {@code UserCreationException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   * @param cause the cause of the exception
   */
  public UserCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
