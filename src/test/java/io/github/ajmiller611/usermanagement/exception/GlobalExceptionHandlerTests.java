package io.github.ajmiller611.usermanagement.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.github.ajmiller611.usermanagement.response.ResponseWrapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * This test class verifies the behavior of exception handling methods in
 * {@code GlobalExceptionHandler} to ensure they produce correct HTTP responses and log messages
 * based on specific exception types.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTests {

  @InjectMocks private GlobalExceptionHandler globalExceptionHandler;
  @Mock private BindingResult bindingResult;

  /**
   * Tests the {@code handleValidationException} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link MethodArgumentNotValidException} results in an HTTP 400 (Bad Request)
   * response with the correct structure and content.
   */
  @Test
  void givenMethodArgumentNotValidExceptionWhenHandleValidationExceptionThenBadRequest() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    FieldError fieldError = mock(FieldError.class);
    when(fieldError.getField()).thenReturn("username");
    when(fieldError.getDefaultMessage()).thenReturn("Username is required");
    when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ResponseWrapper<Map<String, String>>> response =
        globalExceptionHandler.handleValidationException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
        "Expected HTTP status to be 400 BAD_REQUEST for method argument not valid exception");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for method argument not valid exception");

    assertEquals("Validation failed", response.getBody().getMessage(),
        "Expected response message to be 'Validation failed'");

    assertEquals("Username is required", response.getBody().getData().get("username"),
        "Expected validation error for field 'username' to be 'Username is required'");
  }

  /**
   * Tests the {@code handleUnknownProperty} method in {@link GlobalExceptionHandler}.
   * Verifies that an {@link UnrecognizedPropertyException} results in an HTTP 400 (Bad Request)
   * response with the correct structure and content.
   *
   * <p>This test also verifies that a warning log is generated when the unrecognized field
   * resembles a user ID field.
   * </p>
   */
  @Test
  void givenUnrecognizedPropertyExceptionWhenHandleUnknownPropertyThenBadRequest() {
    UnrecognizedPropertyException exception = mock(UnrecognizedPropertyException.class);
    when(exception.getPropertyName()).thenReturn("user_id");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnknownProperty(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
          "Expected HTTP status to be 400 BAD_REQUEST for unrecognized property exception");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' for unrecognized property exception");

      assertEquals("Unrecognized field named 'user_id'.", response.getBody().getMessage(),
          "Expected response message to describe the unrecognized field 'user_id'");

      assertThat(logCaptor.getWarnLogs())
          .withFailMessage("Expected log entry to warn about potential misuse and "
              + "include 'registration request'")
          .anyMatch(log -> log.contains("Potential misuse:")
              && log.contains("registration request"));
    }

    when(exception.getPropertyName()).thenReturn("unknownField");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnknownProperty(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
          "Expected HTTP status to be 400 BAD_REQUEST for unrecognized property exception");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' for unrecognized property exception");

      assertEquals("Unrecognized field named 'unknownField'.", response.getBody().getMessage(),
          "Expected response message to describe the unrecognized field 'unknownField'");

      assertThat(logCaptor.getWarnLogs())
          .withFailMessage("Expected log entry to include unrecognized field name "
              + "and mention 'registration request'")
          .anyMatch(log -> log.contains("Unrecognized field named")
              && log.contains("registration request"));
    }
  }

  /**
   * Tests the {@code handleUserAlreadyExists} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link UserAlreadyExistsException} results in an HTTP 409 (Conflict)
   * response with the correct structure and content.
   */
  @Test
  void givenUserAlreadyExistsExceptionWhenHandleUserAlreadyExistsThenConflict() {
    UserAlreadyExistsException exception = new UserAlreadyExistsException("User already exists");

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleUserAlreadyExists(exception);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
        "Expected HTTP status to be 409 CONFLICT when a user already exists");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for user already exists exception");

    assertThat(response.getBody().getMessage())
        .withFailMessage("Expected response message to contain 'User already exists'")
        .contains("User already exists");
  }

  /**
   * Tests the {@code handleArgumentTypeMismatch} method. Verifies that a
   * {@link MethodArgumentTypeMismatchException} results in an HTTP 400 (Bad Request) response
   * with correct structure and content.
   */
  @Test
  void givenMethodArgumentTypeMismatchExceptionWhenHandleArgumentTypeMismatchThenBadRequest() {
    MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleArgumentTypeMismatch(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
        "Expected HTTP status to be 400 BAD_REQUEST for a type mismatch error");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for method argument type mismatch exception");

    assertEquals("Invalid argument data type", response.getBody().getMessage(),
        "Expected response message to indicate an invalid argument data type");
  }

  /**
   * Tests the {@code handleUserCreationException} method. Verifies that a
   * {@link UserCreationException} results in an HTTP 500 (internal server error) response
   * with correct structure and content.
   */
  @Test
  void givenUserCreationExceptionWhenHandleUserCreationExceptionThenInternalServerError() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataAccessException cause = mock(DataAccessException.class);
      UserCreationException exception =
          new UserCreationException("User Creation Exception message", cause);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUserCreationException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
          "Expected HTTP status to be 500 INTERNAL_SERVER_ERROR for user creation failure");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' for user creation exception");

      assertThat(response.getBody().getMessage())
          .withFailMessage("Expected the response message to indicate a "
              + "user creation failure")
          .contains("User creation failed:");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected logs to include 'DataAccessException' details "
              + "and exception message")
          .anyMatch(log ->
              log.contains("DataAccessException")
                  && log.contains("occurred during user creation with message:")
                  && log.contains("User Creation Exception message"));

      // Unknown cause
      exception = new UserCreationException("User Creation Exception message", null);

      response = globalExceptionHandler.handleUserCreationException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
          "Expected HTTP status to be 500 INTERNAL_SERVER_ERROR for user creation failure "
              + "with unknown cause");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' for UserCreationException with "
              + "unknown cause");

      assertThat(response.getBody().getMessage())
          .withFailMessage("Expected the response message to indicate a "
              + "user creation failure")
          .contains("User creation failed:");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected logs to include 'Unknown Cause' and "
              + "exception message for user creation failure")
          .anyMatch(log ->
                  log.contains("Unknown Cause")
                      && log.contains("occurred during user creation with message:")
                      && log.contains("User Creation Exception message"));
    }
  }

  /**
   * Tests the {@code handleUserNotFoundException} method. Verifies that a
   * {@link UserNotFoundException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenUserNotFoundExceptionWhenHandleUserNotFoundExceptionThenNotFound() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      UserNotFoundException exception = mock(UserNotFoundException.class);
      when(exception.getMessage()).thenReturn("User with id 3 does not exist");
      when(exception.getOperation()).thenReturn("deleteUser");

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUserNotFoundException(exception);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
          "Expected HTTP status to be 404 NOT_FOUND when user is not found");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when user is not found");

      assertEquals("User with id 3 does not exist", response.getBody().getMessage(),
          "Expected response message to indicate that the user with the given ID "
              + "does not exist");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to include UserNotFoundException "
              + "details with the correct user ID and operation")
          .contains("UserNotFoundException: User with id 3 does not exist | Operation: deleteUser");
    }
  }

  /**
   * Tests the {@code handleUnauthorizedOperationException} method. Verifies that a
   * {@link UnauthorizedOperationException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenUnauthorizedOperationExceptionWhenHandleUnauthorizedOperationExceptionThenNotFound() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      UnauthorizedOperationException exception = mock(UnauthorizedOperationException.class);
      when(exception.getMessage()).thenReturn("User with id 3 does not exist");
      when(exception.getId()).thenReturn(3L);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnauthorizedOperationException(exception);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
          "Expected HTTP status to be 404 NOT_FOUND when an unauthorized operation occurs");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when the operation is unauthorized");

      assertEquals("User with id 3 does not exist", response.getBody().getMessage(),
          "Expected response message to indicate that the user with the given ID "
              + "does not exist");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to contain the message about the "
              + "unauthorized operation for the user with ID 3")
          .contains("User with id 3 does not exist");
    }
  }

  /**
   * Tests the {@code handleDataIntegrityViolationException} method. Verifies that a
   * {@link DataIntegrityViolationException} results in an HTTP 409 (conflict) response
   * with correct structure and content.
   */
  @Test
  void givenDataIntegrityViolationExceptionWhenHandleDataIntegrityViolationExceptionThenConflict() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleDataIntegrityViolationException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
          "Expected HTTP status to be 409 CONFLICT when a data integrity violation occurs");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when a data integrity violation occurs");

      assertEquals("A conflict occurred due to database constraints",
          response.getBody().getMessage(),
          "Expected response message to indicate that a conflict occurred due to "
              + "database constraints");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to indicate a "
              + "data integrity violation during the operation")
          .contains("Data integrity violation occurred");
    }
  }

  /**
   * Tests the {@code handleOptimisticLockingFailureException} method. Verifies that a
   * {@link OptimisticLockingFailureException} results in an HTTP 409 (conflict) response
   * with correct structure and content.
   */
  @Test
  void givenOptimisticLockingFailureWhenHandleOptimisticLockingFailureExceptionThenConflict() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      OptimisticLockingFailureException exception = mock(OptimisticLockingFailureException.class);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleOptimisticLockingFailureException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
          "Expected HTTP status to be 409 CONFLICT when an optimistic locking failure occurs");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when an optimistic locking failure occurs");

      assertEquals("Concurrency conflict: another user updated the record",
          response.getBody().getMessage(),
          "Expected response message to indicate a concurrency conflict due to "
              + "another user updating the record");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to indicate an "
              + "optimistic locking conflict during the operation")
          .contains("Optimistic locking conflict");
    }
  }

  /**
   * Tests the {@code handleDataAccessException} method. Verifies that a
   * {@link DataAccessException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenDataAccessExceptionWhenHandleDataAccessExceptionThenInternalServerError() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataAccessException exception = mock(DataAccessException.class);
      when(exception.getMessage()).thenReturn("An unexpected database error occurred");

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleDataAccessException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
          "Expected HTTP status to be 500 INTERNAL SERVER ERROR when a "
              + "DataAccessException occurs");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when a DataAccessException occurs");

      assertEquals("An unexpected database error occurred", response.getBody().getMessage(),
          "Expected response message to contain details of the unexpected database error");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to contain the "
              + "phrase 'Database error occurred' when a DataAccessException is handled")
          .contains("Database error occurred");
    }
  }

  /**
   * Tests the {@code handleHandlerMethodValidationException} method. Verifies that when a
   * {@link ConstraintViolationException} occurs from a Jakarta annotation, a
   * {@link HandlerMethodValidationException} is thrown and wraps a
   * {@link ConstraintViolationException}. After the exception is handled, it results in an
   * HTTP 400 (bad request) response with correct structure and content.
   */
  @Test
  void givenConstraintViolationExceptionWhenHandleHandlerMethodValidationExceptionThenBadRequest() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      Path mockPath = mock(Path.class);
      when(mockPath.toString()).thenReturn("deleteUser.id");

      ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);
      when(mockViolation.getMessage()).thenReturn("User id must be greater than zero");
      when(mockViolation.getInvalidValue()).thenReturn(0L);

      ConstraintViolationException constraintViolationException = new ConstraintViolationException(
          Set.of(mockViolation));

      HandlerMethodValidationException mockHandlerMethodValidationException =
          mock(HandlerMethodValidationException.class);
      when(mockHandlerMethodValidationException.getCause())
          .thenReturn(constraintViolationException);

      ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> response =
          globalExceptionHandler
              .handleHandlerMethodValidationException(mockHandlerMethodValidationException);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
          "Expected HTTP status to be 400 BAD REQUEST when a ConstraintViolationException "
              + "is handled");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when handling a validation exception");

      assertEquals("Validation failed", response.getBody().getMessage(),
          "Expected response message to be 'Validation failed' when handling a "
              + "validation exception");

      assertEquals("id", response.getBody().getData().getFirst().get("field"),
          "Expected field name to be 'id' from the validation error");

      assertEquals("User id must be greater than zero",
          response.getBody().getData().getFirst().get("message"),
          "Expected validation message to be 'User id must be greater than zero'");

      assertEquals(0L, response.getBody().getData().getFirst().get("invalidValue"),
          "Expected invalid value to be '0' from the validation error");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to contain information about the "
              + "ConstraintViolationException")
          .contains("HandlerMethodValidationException caught with cause: "
              + "ConstraintViolationException");

      // Set up when cause is null
      when(mockHandlerMethodValidationException.getCause()).thenReturn(null);

      globalExceptionHandler
          .handleHandlerMethodValidationException(mockHandlerMethodValidationException);

      // Assert that a null cause will return Unknown Cause name in the log
      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to contain 'Unknown Cause' when the "
              + "exception cause is null")
          .contains("HandlerMethodValidationException caught with cause: "
              + "Unknown Cause");
    }
  }

  /**
   * Tests the {@code handleConstraintViolationException} method. Verifies that a
   * {@link ConstraintViolationException} results in an HTTP 400 (bad request) response
   * with correct structure and content.
   */
  @Test
  void givenConstraintViolationExceptionWhenHandleConstraintViolationExceptionThenBadRequest() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      Path mockPath = mock(Path.class);
      when(mockPath.toString()).thenReturn("deleteUser.id");

      ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);
      when(mockViolation.getMessage()).thenReturn("User id must be greater than zero");
      when(mockViolation.getInvalidValue()).thenReturn(0L);

      ConstraintViolationException mockConstraintViolationException =
          mock(ConstraintViolationException.class);
      when(mockConstraintViolationException.getConstraintViolations())
          .thenReturn(Set.of(mockViolation));

      ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> response =
          globalExceptionHandler
              .handleConstraintViolationException(mockConstraintViolationException);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
          "Expected HTTP status to be 400 BAD REQUEST when handling a "
              + "ConstraintViolationException");

      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
          "Expected response status to be 'error' when handling a validation exception");

      assertEquals("Validation failed", response.getBody().getMessage(),
          "Expected response message to be 'Validation failed' when handling a "
              + "validation exception");

      List<Map<String, Object>> errors = response.getBody().getData();
      assertNotNull(errors, "Expected error list to be non-null");
      assertEquals(1, errors.size(), "Expected exactly one validation error in the response");

      Map<String, Object> error = errors.getFirst();
      assertEquals("id", error.get("field"),
          "Expected field name to be 'id' from the validation error");

      assertEquals("User id must be greater than zero", error.get("message"),
          "Expected validation message to be 'User id must be greater than zero'");

      assertEquals(0L, error.get("invalidValue"),
          "Expected invalid value to be '0' from the validation error");

      assertThat(logCaptor.getErrorLogs())
          .withFailMessage("Expected error logs to contain details about the validation failure")
          .contains("Validation failure: deleteUser failed with id having invalid value of 0 with "
              + "message User id must be greater than zero");
    }
  }

  /**
   * Tests the {@code handleInvitationNotFoundException} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link InvitationNotFoundException} results in an HTTP 404 (Not Found)
   * response with the correct structure and content.
   */
  @Test
  void givenInvitationNotFoundExceptionWhenHandleInvitationNotFoundThenNotFound() {
    InvitationNotFoundException exception = new InvitationNotFoundException("Invitation not found");

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleInvitationNotFoundException(exception);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
        "Expected HTTP status to be 404 NOT FOUND when an invitation could not be found");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for invitation not found exception");

    assertThat(response.getBody().getMessage())
        .withFailMessage("Expected response message to contain 'Invitation not found'")
        .contains("Invitation not found");
  }

  /**
   * Test the {@code handleInvitationAlreadyUsedException} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link InvitationAlreadyUsedException} results in an HTTP 409 (Conflict)
   * response with the correct structure and content.
   */
  @Test
  void givenInvitationAlreadyUsedExceptionWhenHandleInvitationAlreadyUsedThenNotFound() {
    InvitationAlreadyUsedException exception =
        new InvitationAlreadyUsedException("Invitation is already used");

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleInvitationAlreadyUsedException(exception);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
        "Expected HTTP status to be 409 CONFLICT when an invitation could not be found");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for invitation not found exception");

    assertThat(response.getBody().getMessage())
        .withFailMessage("Expected response message to contain 'Invitation is already used'")
        .contains("Invitation is already used");
  }

  /**
   * Tests the {@code handleInvitationExpiredException} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link InvitationExpiredException} results in an HTTP 400 (Bad Request)
   * response with the correct structure and content.
   */
  @Test
  void givenInvitationExpiredExceptionWhenHandleInvitationExpiredThenNotFound() {
    InvitationExpiredException exception = new InvitationExpiredException("Invitation is expired");

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleInvitationExpiredException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
        "Expected HTTP status to be 400 BAD REQUEST when an invitation could not be found");

    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus(),
        "Expected response status to be 'error' for invitation not found exception");

    assertThat(response.getBody().getMessage())
        .withFailMessage("Expected response message to contain 'Invitation is expired'")
        .contains("Invitation is expired");
  }
}
