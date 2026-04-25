package io.github.ajmiller611.usermanagement.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A generic wrapper class for standardizing API responses.
 *
 * <p>This class encapsulates the status, message, and data fields in API responses, providing
 * a consistent structure for both success and error responses. It includes utility methods
 * for easily creating success or error responses.</p>
 *
 * @param <T> the type of the data field in the response.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseWrapper<T> {

  private String status;
  private String message;
  private T data;

  /**
   * Creates a success response with the specified data and message.
   *
   * @param data    the data payload to include in the response.
   * @param message a descriptive message indicating the success of the operation.
   * @param <T>     the type of the data payload.
   * @return a {@link ResponseWrapper} object representing the success response.
   */
  public static <T> ResponseWrapper<T> success(T data, String message) {
    return new ResponseWrapper<>("success", message, data);
  }

  /**
   * Creates an error response with the specified message.
   *
   * @param message a descriptive message indicating the error that occurred.
   * @param <T>     the type of the data payload (which will be {@code null} for error responses).
   * @return a {@link ResponseWrapper} object representing the error response.
   */
  public static <T> ResponseWrapper<T> error(String message) {
    return new ResponseWrapper<>("error", message, null);
  }
}
