package io.github.ajmiller611.usermanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ajmiller611.usermanagement.config.AppConfig;
import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.dto.InvitationRequestDto;
import io.github.ajmiller611.usermanagement.dto.InvitationResponseDto;
import io.github.ajmiller611.usermanagement.security.JwtService;
import io.github.ajmiller611.usermanagement.service.InvitationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link InvitationController}.
 *
 * <p>This test class focuses on verifying how the controller handles HTTP requests
 * for creating invitations.</p>
 *
 * <h2>What these tests cover</h2>
 * <ul>
 *   <li>
 *     <strong>Security rules:</strong> Ensures only ADMIN users can access the create invitation
 *     endpoint, and non-admin users receive a 403 Forbidden response.
 *   </li>
 *   <li>
 *     <strong>Successful request handling:</strong> Verifies that a valid request returns a
 *     201 Created response with the expected token, email, and expiration date from the service.
 *   </li>
 *   <li>
 *     <strong>Validation checks:</strong> Ensures invalid requests (missing email, missing role,
 *     or invalid email format) return a 400 Bad Request and that the service method is not called.
 *   </li>
 * </ul>
 */
@WebMvcTest(InvitationController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class InvitationControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private InvitationService invitationService;
  @MockBean private JwtService jwtService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private AuthenticationManager authenticationManager;

  /** Verifies a non-admin user request is responded with forbidden (403). */
  @Test
  @WithMockUser
  void givenNonAdminUserWhenGetInvitationThenReturnForbidden() throws Exception {
    mockMvc.perform(post("/invitations")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  /**
   * Verifies a valid request is successfully processed and responded with the token
   * and metadata in the response.
   */
  @Test
  @WithMockUser(roles = "ADMIN")
  void givenValidRequestWhenCreateInvitationThenReturnCreatedResponse() throws Exception {
    InvitationRequestDto invitationRequestDto = new InvitationRequestDto(
        "test@email.com",
        "USER"
    );

    String validJson = objectMapper.writeValueAsString(invitationRequestDto);

    LocalDateTime expiresAtTimestamp = LocalDateTime.of(2026, 5, 29, 0, 0, 0, 0);
    InvitationResponseDto  invitationResponseDto = new InvitationResponseDto(
        "token",
        "test@email.com",
        expiresAtTimestamp
    );

    when(invitationService.createInvitation(any(InvitationRequestDto.class)))
        .thenReturn(invitationResponseDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(InvitationController.class)) {
      mockMvc.perform(post("/invitations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.token").value(invitationResponseDto.getToken()))
          .andExpect(jsonPath("$.email").value(invitationResponseDto.getEmail()))
          .andExpect(jsonPath("$.expiresAt")
              .value(expiresAtTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

      assertThat(logCaptor.getInfoLogs())
          .withFailMessage("")
          .anyMatch(log -> log.contains("Endpoint /invitations received POST request: ")
              && log.contains(invitationRequestDto.getEmail())
              && log.contains(invitationRequestDto.getRole()));
    }
  }

  /** Verifies a missing email in the request responds with bad request (400). */
  @Test
  @WithMockUser(roles = "ADMIN")
  void givenMissingEmailWhenGetInvitationThenReturnBadRequest() throws Exception {
    InvitationRequestDto invitationRequestDto = new InvitationRequestDto(
        null,
        "USER"
    );
    String validJson = objectMapper.writeValueAsString(invitationRequestDto);

    mockMvc.perform(post("/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isBadRequest());

    verify(invitationService, never()).createInvitation(any(InvitationRequestDto.class));
  }

  /** Verifies an invalid email in the request responds with bad request (400). */
  @Test
  @WithMockUser(roles = "ADMIN")
  void givenInvalidEmailWhenGetInvitationThenReturnBadRequest() throws Exception {
    InvitationRequestDto invitationRequestDto = new InvitationRequestDto(
        "invalidEmail",
        "USER"
    );

    String validJson = objectMapper.writeValueAsString(invitationRequestDto);

    mockMvc.perform(post("/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isBadRequest());

    verify(invitationService, never()).createInvitation(any(InvitationRequestDto.class));
  }

  /** Verifies a missing role in the request responds with a bad request (400). */
  @Test
  @WithMockUser(roles = "ADMIN")
  void givenMissingRoleWhenGetInvitationThenReturnBdRequest() throws Exception {
    InvitationRequestDto invitationRequestDto = new InvitationRequestDto(
        "test@email.com",
        null
    );

    String validJson = objectMapper.writeValueAsString(invitationRequestDto);

    mockMvc.perform(post("/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isBadRequest());

    verify(invitationService, never()).createInvitation(any(InvitationRequestDto.class));
  }


}
