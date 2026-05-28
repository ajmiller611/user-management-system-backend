package io.github.ajmiller611.usermanagement.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ajmiller611.usermanagement.config.AppConfig;
import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.controller.UserController;
import io.github.ajmiller611.usermanagement.dto.UserResponseDto;
import io.github.ajmiller611.usermanagement.dto.UserUpdateRequestDto;
import io.github.ajmiller611.usermanagement.repository.RoleRepository;
import io.github.ajmiller611.usermanagement.repository.UserRepository;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.UserService;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link UserController} class.
 *
 * <p>This test class verifies the functionality of the {@code UserController}'s
 * update user endpoint, focusing on handling valid user update requests, integration with
 * validation annotations, response structure consistency, and logging behavior.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Valid User Update:</strong> Ensures that a valid user update request returns an
 *     HTTP 200 status, along with the updated user details in the response.
 *   </li>
 *   <li>
 *     <strong>Validation Integration:</strong> Confirms that invalid user input, such as a username
 *     that does not meet the requirements, results in an HTTP 400 status with an appropriate
 *     validation error message. Also verifies that the service layer is not called when validation
 *     fails.
 *   </li>
 *   <li>
 *     <strong>Response Structure Consistency:</strong> Validates that the response follows the
 *     standard structure, including status, message, and data fields.
 *   </li>
 *   <li>
 *     <strong>Logging Behavior:</strong> Ensures that requests to the update endpoint are logged
 *     with the expected details for monitoring and debugging purposes.
 *   </li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class UserControllerUpdateTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private UserService userService;
  @MockBean private JwtService jwtService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private UserRepository userRepository;

  Long testUserId;
  UserUpdateRequestDto requestDto;
  UserResponseDto responseDto;
  String validJson;

  /** Initializes test data before each test case. */
  @BeforeEach
  void setUp() throws JsonProcessingException {
    testUserId = 2L;
    String updatedUsername = "updatedUsername";
    String updatedEmail = "updatedEmail@example.com";

    requestDto = new UserUpdateRequestDto(updatedUsername, updatedEmail);
    validJson = objectMapper.writeValueAsString(requestDto);

    responseDto = new UserResponseDto(
        testUserId,
        updatedUsername,
        updatedEmail
    );
  }

  /**
   * Verifies that a valid user's update request returns ok (200) with updated user details
   * and logs properly.
   */
  @Test
  void givenValidUpdateRequestAndUserIdWhenUpdateUserThenReturnsSuccessWithUpdatedUserDetails()
      throws Exception {

    // Mockito uses strict argument matching, and deserialization creates a new UserUpdateRequestDto
    // instance, causing exact matches to fail. Using argument matchers (e.g., eq() and any())
    // ensures the service mock recognizes the ID and request DTO regardless of instance identity.
    when(userService.updateUser(eq(testUserId), any(UserUpdateRequestDto.class)))
        .thenReturn(responseDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(UserController.class)) {
      mockMvc.perform(put("/users/2")
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message").value("User updated successfully"))
          .andExpect(jsonPath("$.data.userId").value(2L))
          .andExpect(jsonPath("$.data.username").value("updatedUsername"))
          .andExpect(jsonPath("$.data.email")
              .value("updatedEmail@example.com"));

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("Expected log to contain PUT request log for user ID 2 "
              + "but it was missing.")
          .contains("Endpoint '/users/{id}' received PUT request with id = 2");
    }
  }

  /**
   * Verifies that an invalid username returns a bad request (400).
   * This test case verifies that validation annotations are integrated.
   */
  @Test
  void givenInvalidUsernameWhenUpdateUserThenReturnBadRequest() throws Exception {
    requestDto.setUsername("in");
    String invalidUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(put("/users/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));

    // Verifies the validation annotation threw an exception and the user service was not invoked.
    verify(userService, never())
        .updateUser(any(Long.class), any(UserUpdateRequestDto.class));
  }
}
