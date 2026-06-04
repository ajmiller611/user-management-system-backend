package io.github.ajmiller611.usermanagement.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ajmiller611.usermanagement.config.RsaKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link JwtAuthenticationFilter}. This class tests the functionality of the
 * JWT authentication filter to ensure it processes incoming requests with valid and invalid
 * JWT tokens correctly, passing requests to the next filter or responding with an Unauthorized
 * status as needed.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTests {

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;
  @Mock private JwtService jwtService;
  @Mock private JwtAuthenticationConverter jwtAuthenticationConverter;
  @Mock private JwtDecoder jwtDecoder;
  @Mock private FilterChain filterChain;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private static String validToken;
  private final String invalidToken = "invalid.jwt.token";

  /**
   * This method is executed once before all test methods in the class are run.
   * It generates a valid JWT token which is reused in the test methods.
   */
  @BeforeAll
  static void setUpOnce() {
    RsaKeyProperties keys = new RsaKeyProperties();
    JwtService jwtServiceSetUp = new JwtService(keys);

    // Generate a valid token
    String username = "testUser";
    Collection<? extends GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("USER"));
    Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
    validToken = jwtServiceSetUp.generateAccessToken(auth);
  }

  /**
   * Sets up the necessary mock and reset the security context.
   * This method runs before each test method to initialize the required objects and variables.
   */
  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext(); // Reset security context

    jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, jwtAuthenticationConverter);
  }

  /**
   * Test to ensure that when no Authorization header is provided in the request,
   * the request is passed to the next filter without any processing.
   */
  @Test
  void givenNoAuthorizationHeaderWhenFilterIsExecutedThenRequestIsPassedToNextFilter()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("null");

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  /**
   * Test to ensure that when an invalid Authorization header is provided in the request,
   * the request is passed to the next filter without any processing.
   */
  @Test
  void givenInvalidAuthorizationHeaderWhenFilterIsExecutedThenRequestIsPassedToNextFilter()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Invalid Token");

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  /**
   * Test to ensure that when a valid Authorization header is provided in the request,
   * the JWT token is decoded, an authentication token is created, and the authentication
   * is set in the security context.
   */
  @Test
  void givenValidAuthorizationHeaderWhenFilterIsExecutedThenAuthenticationIsSet()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
    when(jwtService.jwtDecoder()).thenReturn(jwtDecoder);
    Jwt jwt = mock(Jwt.class);
    JwtAuthenticationToken authenticationToken = mock(JwtAuthenticationToken.class);
    when(jwtService.jwtDecoder().decode(validToken)).thenReturn(jwt);
    when(jwtAuthenticationConverter.convert(jwt)).thenReturn(authenticationToken);

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    verify(jwtAuthenticationConverter).convert(jwt);
    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
        "Expected authentication to be set in SecurityContextHolder after filter execution");
  }

  /**
   * Test to ensure that when an invalid JWT token is provided in the Authorization header,
   * the filter responds with a 401 Unauthorized status and does not pass the request to the
   * next filter.
   */
  @Test
  void givenInvalidJwtTokenWhenFilterIsExecutedThenRespondUnauthorized()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
    when(jwtService.jwtDecoder()).thenReturn(jwtDecoder);
    when(jwtService.jwtDecoder().decode(invalidToken))
        .thenThrow(new JwtException("Invalid Token"));

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    assertThrows(JwtException.class,
        () -> jwtDecoder.decode(invalidToken),
        "Expected decode to throw an exception when invalid token");

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
    verifyNoInteractions(filterChain);
  }

  /**
   * Test to ensure that when an authenticated user is already present in the security context,
   * the filter does not attempt to process the JWT token and simply passes the request to the
   * next filter.
   */
  @Test
  void givenAuthenticatedUserWhenFilterIsExecutedThenRequestIsPassedToNextFilter()
      throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
    SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(jwtService, jwtAuthenticationConverter);
  }
}
