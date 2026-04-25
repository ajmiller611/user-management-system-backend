package io.github.ajmiller611.usermanagement.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.github.ajmiller611.usermanagement.response.ResponseWrapper;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for handling application-wide exceptions.
 * This class provides specific handlers for validation errors, unrecognized JSON fields,
 * and user existence conflicts.
 *
 * <p>All exception handlers in this class return consistent error responses with HTTP
 * status codes and detailed error messages, ensuring a standardized format for error handling.
 * </p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String GENERIC_VALIDATION_FAILED_ERROR_MESSAGE = "Validation failed";
  private static final String UNKNOWN_CAUSE_NAME = "Unknown Cause";

  /**
   * Handles validation exceptions thrown when method arguments fail validation.
   * Captures details about invalid fields and provides a structured error response.
   *
   * @param ex the {@link MethodArgumentNotValidException} containing validation error details
   * @return a {@link ResponseEntity} containing the error response with HTTP status 400
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseWrapper<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException ex) {

    Map<String, String> details = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      details.put(fieldName, errorMessage);
    });

    ResponseWrapper<Map<String, String>> response =
        ResponseWrapper.error(GENERIC_VALIDATION_FAILED_ERROR_MESSAGE);
    response.setData(details);
    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles exceptions for unrecognized properties in JSON requests.
   * If the unrecognized property resembles a user ID field, logs a warning about
   * potential misuse then provides a structured error response.
   *
   * @param ex the {@link UnrecognizedPropertyException} containing details about the unrecognized
   *           property
   * @return a {@link ResponseEntity} containing the error response with HTTP status 400
   */
  @ExceptionHandler(UnrecognizedPropertyException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUnknownProperty(
      UnrecognizedPropertyException ex) {
    String propertyName = ex.getPropertyName();

    List<String> userIdFields = Arrays.asList(
        "userId", "userid", "userID", "user_Id", "user_id", "user-Id", "user-id", "user-ID");

    if (userIdFields.contains(propertyName)) {
      logger.warn("Potential misuse: A '{}' field was found in a registration request.",
          propertyName);
    } else {
      logger.warn("Unrecognized field named '{}' found in a registration request.", propertyName);
    }

    return ResponseEntity.badRequest().body(
        ResponseWrapper.error(String.format("Unrecognized field named '%s'.", propertyName))
    );
  }

  /**
   * Handles exceptions thrown when a user already exists during registration then provides a
   * structured error response.
   *
   * @param ex the {@link UserAlreadyExistsException} containing error details about the existing
   *           user conflict
   * @return a {@link ResponseEntity} containing the error response with HTTP status 409
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ResponseWrapper.error(ex.getMessage())
    );
  }

  /**
   * Handles exceptions for {@link MethodArgumentTypeMismatchException} when arguments are
   * the wrong type.
   *
   * @param ex the {@link MethodArgumentTypeMismatchException} containing error details about
   *           the type mismatch
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 400
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseWrapper<String>> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    return ResponseEntity.badRequest().body(
        ResponseWrapper.error("Invalid argument data type")
    );
  }

  /**
   * Handles exceptions for {@link UserCreationException} when an error occurs during
   * a save user to the database call.
   *
   * @param ex the {@link UserCreationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 500
   */
  @ExceptionHandler(UserCreationException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserCreationException(
      UserCreationException ex) {
    String causeType =
        ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : UNKNOWN_CAUSE_NAME;
    logger.error("{} occurred during user creation with message: {}",
        causeType, ex.getMessage());
    return ResponseEntity.internalServerError().body(
        ResponseWrapper.error("User creation failed: " + ex.getMessage())
    );
  }

  /**
   * Handles {@link UserNotFoundException} when a user does not exist in the database.
   *
   * @param ex the {@link UserNotFoundException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 404
   */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserNotFoundException(
      UserNotFoundException ex) {
    logger.error("UserNotFoundException: {} | Operation: {}", ex.getMessage(), ex.getOperation());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ResponseWrapper.error(ex.getMessage())
    );
  }

  /**
   * Handles {@link UnauthorizedOperationException} when an unauthorized user attempts an operation
   * on an admin user. The error message must be the exact format as a user not found error message
   * to prevent inference that id used belongs to an admin.
   *
   * @param ex the {@link UnauthorizedOperationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 404
   */
  @ExceptionHandler(UnauthorizedOperationException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUnauthorizedOperationException(
      UnauthorizedOperationException ex) {
    logger.error(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ResponseWrapper.error(String.format("User with id %d does not exist", ex.getId()))
    );
  }

  /**
   * Handles {@link DataIntegrityViolationException} when database constraints are violated.
   *
   * @param ex the {@link DataIntegrityViolationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 409
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ResponseWrapper<String>> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    logger.error("Data integrity violation occurred", ex);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ResponseWrapper.error("A conflict occurred due to database constraints")
    );
  }

  /**
   * Handles {@link OptimisticLockingFailureException} when an optimistic locking conflict occurs.
   *
   * @param ex the {@link OptimisticLockingFailureException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 409
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ResponseWrapper<String>> handleOptimisticLockingFailureException(
      OptimisticLockingFailureException ex) {
    logger.error("Optimistic locking conflict", ex);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ResponseWrapper.error("Concurrency conflict: another user updated the record")
    );
  }

  /**
   * Handles the generic database-related {@link DataAccessException} with a generic error response.
   *
   * @param ex the {@link DataAccessException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 500
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ResponseWrapper<String>> handleDataAccessException(
      DataAccessException ex) {
    logger.error("Database error occurred", ex);
    return ResponseEntity.internalServerError().body(
        ResponseWrapper.error("An unexpected database error occurred")
    );
  }

  /**
   * Handles Spring's method-level validation exceptions, thrown as
   * {@link HandlerMethodValidationException}. This exception is commonly used as a wrapper for
   * exceptions thrown by validation annotations. This method inspects the cause of the exception,
   * delegating the handling to the appropriate exception handler; otherwise, it returns a
   * generic error response.
   *
   * @param ex the {@link HandlerMethodValidationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 400
   */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>>
      handleHandlerMethodValidationException(HandlerMethodValidationException ex) {

    logger.error("HandlerMethodValidationException caught with cause: {}",
        ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : UNKNOWN_CAUSE_NAME);

    // Combine type checking and variable assignment using pattern variables
    if (ex.getCause() instanceof ConstraintViolationException exception) {
      return handleConstraintViolationException(exception);
    }

    // Return a generic error response for other causes
    return ResponseEntity.badRequest().body(
        ResponseWrapper.error(GENERIC_VALIDATION_FAILED_ERROR_MESSAGE)
    );
  }

  /**
   * Handles {@link ConstraintViolationException} thrown by the validation framework.
   *
   * <p>This exception is commonly thrown by validation annotations and wrapped by a
   * {@link HandlerMethodValidationException}. This method will handle cases where the
   * {@link ConstraintViolationException} is manually thrown.
   * </p>
   *
   * @param ex the {@link ConstraintViolationException} containing the error details
   * @return a {@link ResponseEntity} containing the {@link List} of violations with the
   *     HTTP status of 400
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>>
      handleConstraintViolationException(ConstraintViolationException ex) {

    List<Map<String, Object>> errors = ex.getConstraintViolations()
        .stream()
        .map(violation -> {
          Map<String, Object> error = new LinkedHashMap<>();
          String[] propertyPath = violation.getPropertyPath().toString().split("\\.");
          String pathVariableName = propertyPath[1];
          error.put("field", pathVariableName);
          error.put("message", violation.getMessage());
          error.put("invalidValue", violation.getInvalidValue());

          String methodName = propertyPath[0];
          logger.error(
              "Validation failure: {} failed with {} having invalid value of {} with message {}",
              methodName, pathVariableName, violation.getInvalidValue(), violation.getMessage());

          return Collections.unmodifiableMap(error);
        })
        .toList();

    ResponseWrapper<List<Map<String, Object>>> response =
        ResponseWrapper.error(GENERIC_VALIDATION_FAILED_ERROR_MESSAGE);
    response.setData(errors);
    return ResponseEntity.badRequest().body(response);
  }
}
