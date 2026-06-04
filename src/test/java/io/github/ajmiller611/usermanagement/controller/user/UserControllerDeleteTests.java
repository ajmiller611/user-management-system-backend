package io.github.ajmiller611.usermanagement.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ajmiller611.usermanagement.config.AppConfig;
import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.controller.UserController;
import io.github.ajmiller611.usermanagement.exception.UserNotFoundException;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.UserService;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link UserController} class.
 *
 * <p>This test class verifies the functionality of the {@code UserController}'s
 * delete user endpoint, focusing on ensuring the correct handling of valid and
 * invalid delete requests, as well as appropriate error responses for different scenarios.</p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Valid User Deletion:</strong> Ensures that a valid delete request for an existing
 *     user results in a successful response (status 200) with the appropriate success message.
 *   </li>
 *   <li>
 *     <strong>Invalid User ID Handling:</strong> Verifies that when an invalid user ID
 *     is provided, the controller responds with a bad request (status 400) and an appropriate
 *     error message, and no service layer invocation occurs.
 *   </li>
 *   <li>
 *     <strong>Non-Existent User Handling:</strong> Confirms that attempting to delete a user
 *     with a non-existent ID results in a not found response (status 404) with a descriptive
 *     error message.
 *   </li>
 *   <li>
 *     <strong>Logging Behavior:</strong> Confirms that the controller logs appropriate messages
 *     for successful and unsuccessful requests, including detailed error logs for exceptions
 *     encountered during the delete operation.
 *   </li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class UserControllerDeleteTests {

  @Autowired private MockMvc mockMvc;
  @MockBean private UserService userService;
  @MockBean private JwtService jwtService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private UserRepository userRepository;

  /**
   * Verifies that a valid user id returns ok (200) and logs the request correctly.
   */
  @Test
  void givenValidUserIdWhenDeleteUserThenReturnSuccess() throws Exception {
    try (LogCaptor logCaptor = LogCaptor.forClass(UserController.class)) {
      Long validUserId = 2L;
      doNothing().when(userService).deleteUser(validUserId);

      mockMvc.perform(delete("/users/2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message").value("User deleted successfully"));

      verify(userService, times(1)).deleteUser(validUserId);

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log to contain the message for DELETE request "
              + "with id = 2, but it was not found.")
          .contains("Endpoint '/users/{id}' received DELETE request with id = 2");
    }
  }

  /**
   * Verifies that invalid user id returns a bad request (400).
   */
  @Test
  void givenInvalidUserIdWhenDeleteUserThenReturnBadRequest() throws Exception {
    Long invalidUserId = -1L;
    mockMvc.perform(delete("/users/-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.[0].field").value("id"))
        .andExpect(jsonPath("$.data.[0].message")
            .value("User id must be greater than zero"))
        .andExpect(jsonPath("$.data.[0].invalidValue").value(invalidUserId));

    verify(userService, never()).deleteUser(invalidUserId);

    invalidUserId = 0L;
    mockMvc.perform(delete("/users/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.[0].field").value("id"))
        .andExpect(jsonPath("$.data.[0].message")
            .value("User id must be greater than zero"))
        .andExpect(jsonPath("$.data.[0].invalidValue").value(invalidUserId));

    verify(userService, never()).deleteUser(invalidUserId);
  }

  /**
   * Verifies that non-existing user id returns a bad request (400).
   */
  @Test
  void givenNonExistingUserIdWhenDeleteUserThenThrowsUserNotFoundException() throws Exception {
    Long nonExistentUserId = 3L;
    doThrow(new UserNotFoundException("User with id 3 does not exist", "deleteUser"))
        .when(userService).deleteUser(nonExistentUserId);

    mockMvc.perform(delete("/users/3"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User with id 3 does not exist"));

    verify(userService, times(1)).deleteUser(nonExistentUserId);
  }
}
