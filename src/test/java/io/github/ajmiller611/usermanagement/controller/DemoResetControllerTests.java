package io.github.ajmiller611.usermanagement.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ajmiller611.usermanagement.config.SecurityConfig;
import io.github.ajmiller611.usermanagement.security.TokenService;
import io.github.ajmiller611.usermanagement.service.DemoResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link DemoResetController}.
 *
 * <p>Validates that the demo reset endpoint behaves correctly, including:
 * successful execution for authorized users and proper access control
 * for unauthorized requests.</p>
 */
@WebMvcTest(DemoResetController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class DemoResetControllerTests {

  @Autowired private MockMvc mockMvc;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private TokenService tokenService;
  @MockBean private UserDetailsService userDetailsService;
  @MockBean private DemoResetService demoResetService;

  /**
   * Verifies that an authenticated ADMIN user can successfully
   * trigger the demo reset process.
   */
  @Test
  @WithMockUser(roles = "ADMIN")
  void givenAdminUser_whenResetDemo_thenReturnOk() throws Exception {
    mockMvc.perform(post("/admin/demo/reset"))
        .andExpect(status().isOk())
        .andExpect(content().string("Demo data reset successfully."));

    verify(demoResetService).resetDemoData();
  }

  /**
   * Verifies that a non-admin user is forbidden from accessing
   * the demo reset endpoint.
   */
  @Test
  @WithMockUser(roles = "USER")
  void givenNonAdminUser_whenResetDemo_thenReturnForbidden() throws Exception {
    mockMvc.perform(post("/admin/demo/reset"))
        .andExpect(status().isForbidden());
  }

  /**
   * Verifies that an unauthenticated request is rejected
   * when attempting to access the demo reset endpoint.
   */
  @Test
  void givenNoAuthentication_whenResetDemo_thenReturnUnauthorized() throws Exception {
    mockMvc.perform(post("/admin/demo/reset"))
        .andExpect(status().isUnauthorized());
  }
}
