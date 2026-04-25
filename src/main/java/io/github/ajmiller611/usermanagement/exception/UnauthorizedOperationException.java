package io.github.ajmiller611.usermanagement.exception;

import lombok.Getter;

/**
 * Custom exception thrown when an unauthorized user attempts to perform an operation on protected
 * data.
 */
@Getter
public class UnauthorizedOperationException extends RuntimeException {
  private final Long id;

  /**
   * Constructs a new {@code UnauthorizedOperationException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   * @param id the id specified during the operation
   */
  public UnauthorizedOperationException(String message, Long id) {
    super(message);
    this.id = id;
  }
}
