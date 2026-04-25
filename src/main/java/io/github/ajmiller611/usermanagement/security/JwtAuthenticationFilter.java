package io.github.ajmiller611.usermanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JwtAuthenticationFilter is a filter that processes incoming HTTP requests to check and
 * authenticate JWT tokens. It is executed once per request to ensure that requests with a
 * valid JWT token are authenticated, and those without or with an invalid token are denied access
 * with an appropriate error response.
 *
 * <p>The filter checks for the presence of an "Authorization" header with a JWT token, decodes it,
 * and if valid, sets the authentication in the security context. Invalid tokens result in a
 * 401 Unauthorized error.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final TokenService tokenService;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  /**
   * The filter method that intercepts the request and processes the JWT token.
   * If the token is valid, it authenticates the user and continues the filter chain;
   * otherwise, it returns a 401 Unauthorized response.
   *
   * @param request The HttpServletRequest containing the request details.
   * @param response The HttpServletResponse to send back the response.
   * @param filterChain The chain of filters to pass the request through if the token is valid.
   * @throws ServletException If an error occurs during the filtering process.
   * @throws IOException If an I/O error occurs during the filtering process.
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    // Extract the Authorization header from the incoming request.
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    /*
     * Check if the Authorization header is missing, does not start with "Bearer ",
     * or if the user is already authenticated.
     */
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")
        || SecurityContextHolder.getContext().getAuthentication() != null) {
      // If any condition is true, continue to the next filter without processing.
      filterChain.doFilter(request, response);
      return;
    }

    // Extract the token by removing the "Bearer " prefix.
    String token = authorizationHeader.substring(7);

    try {
      // Decode the JWT token using the TokenService.
      Jwt jwt = tokenService.jwtDecoder().decode(token);

      // Convert the decoded JWT into an Authentication token using the JwtAuthenticationConverter.
      JwtAuthenticationToken authentication =
          (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);

      // Set the authentication in the security context.
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (JwtException e) {
      // If decoding the JWT fails, send a 401 Unauthorized response.
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
      return;
    }

    // Continue the filter chain after the authentication is set.
    filterChain.doFilter(request, response);
  }
}
