package io.github.ajmiller611.usermanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ajmiller611.usermanagement.config.RsaKeyProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link JwtService} class.
 *
 * <p>This test class verifies the functionality of the {@code JwtService} methods for generating,
 * decoding, and validating JWT tokens. It ensures that the JWTs generated have valid claims,
 * expected roles, and correct expiration times. The tests also check the configuration of
 * {@link JwtAuthenticationConverter} for appropriate role prefixing.
 *
 * <p>Tests in this class include:
 * <ul>
 *   <li>{@code jwtEncoder()} and {@code jwtDecoder()} methods returning valid encoders/decoders
 *   </li>
 *   <li>Generating access and refresh tokens with proper claims and expiration times</li>
 *   <li>Decoding JWT tokens to verify claims</li>
 *   <li>Validating {@link JwtAuthenticationConverter} configuration for correct role prefixes</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtServiceTests {

  @InjectMocks private JwtService jwtService;

  Authentication auth;
  String username;

  /**
   * Sets up the testing environment before each test case.
   * Initializes {@code JwtService} with mock RSA keys and configures
   * {@code Authentication} with a sample username and authority.
   */
  @BeforeEach
  void setUp() {
    RsaKeyProperties keys = new RsaKeyProperties();
    jwtService = new JwtService(keys);

    username = "testUser";
    Collection<? extends GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("USER"));

    auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
  }

  /**
   * Verifies that the {@code jwtEncoder()} method returns a valid instance
   * of {@link NimbusJwtEncoder}.
   */
  @Test
  void givenValidKeysWhenJwtEncoderThenReturnValidJwtEncoder() {
    JwtEncoder jwtEncoder = jwtService.jwtEncoder();

    assertNotNull(jwtEncoder, "JwtEncoder should not be null");
    assertInstanceOf(NimbusJwtEncoder.class, jwtEncoder,
        "JwtEncoder should be an instance of NimbusJwtEncoder");
  }

  /**
   * Verifies that the {@code jwtDecoder()} method returns a valid instance
   * of {@link NimbusJwtDecoder}.
   */
  @Test
  void givenValidKeysWhenJwtDecoderThenReturnValidJwtDecoder() {
    JwtDecoder jwtDecoder = jwtService.jwtDecoder();

    assertNotNull(jwtDecoder, "JwtDecoder should not be null");
    assertInstanceOf(NimbusJwtDecoder.class, jwtDecoder,
        "JwtDecoder should be an instance of NimbusJwtDecoder");
  }

  /**
   * Tests {@code generateAccessToken()} to ensure the access token generated
   * has valid claims, including the subject, issuer, roles, and expiration time.
   * Asserts that the expiration time is within one hour.
   */
  @Test
  void givenValidAuthenticationWhenGenerateAccessTokenThenReturnAccessTokenString() {
    String token = jwtService.generateAccessToken(auth);
    assertNotNull(token, "Access token should not be null");

    Map<String, Object> claims = jwtService.decodeJwt(token);
    assertEquals(username, claims.get("sub"), "Subject claim should match the username");
    assertEquals("self", claims.get("iss"), "Issuer claim should be 'self'");
    assertEquals("USER", claims.get("roles"), "Roles claim should match 'USER'");

    Instant now = Instant.now();
    Instant expirationTime = now.plus(15, ChronoUnit.MINUTES);
    Instant expiresAt = (Instant) claims.get("exp");
    assertNotNull(expiresAt, "Expiration claim should be present");
    /*
     * Since there will be a minor difference between the expiresAt timestamps, check that
     * the claim's expiresAt fits in the range of an hour.
     */
    assertTrue(expiresAt.isBefore(expirationTime) && expiresAt.isAfter(now),
        "Token expiration time should be within 15 minutes from now");
  }

  /**
   * Tests {@code generateRefreshToken()} to ensure the refresh token generated
   * contains valid claims, including the subject, issuer, and expiration time.
   * Asserts that the expiration time is within seven days.
   */
  @Test
  void givenValidAuthenticationWhenGenerateRefreshTokenThenReturnRefreshTokenString() {
    String token = jwtService.generateRefreshToken(auth);
    assertNotNull(token, "Refresh token should not be null");

    Map<String, Object> claims = jwtService.decodeJwt(token);
    assertEquals(username, claims.get("sub"), "Subject claim should match the username");
    assertEquals("self", claims.get("iss"), "Issuer claim should be 'self'");

    Instant now = Instant.now();
    Instant expirationTime = now.plus(7, ChronoUnit.DAYS);
    Instant expiresAt = (Instant) claims.get("exp");
    assertNotNull(expiresAt, "Expiration claim should be present");
    assertTrue(expiresAt.isBefore(expirationTime) && expiresAt.isAfter(now),
        "Token expiration time should be within 7 days from now");
  }

  /**
   * Tests {@code generateTokens()} to verify that it returns a map of tokens containing
   * both access and refresh tokens. Validates claims and expiration times of each token.
   */
  @Test
  void givenValidAuthenticationWhenGenerateTokensThenReturnMapOfStringTokens() {
    Map<String, String> tokens = jwtService.generateTokens(auth);
    assertNotNull(tokens, "Token map should not be null");

    Map<String, Object> accessTokenClaims = jwtService.decodeJwt(tokens.get("accessToken"));
    assertNotNull(accessTokenClaims, "Access token claims should not be null");
    assertEquals(username, accessTokenClaims.get("sub"), "Subject claim should match the username");
    assertEquals("self", accessTokenClaims.get("iss"), "Issuer claim should be 'self'");
    assertEquals("USER", accessTokenClaims.get("roles"), "Roles claim should match 'USER'");

    Instant now = Instant.now();
    Instant expirationTime = now.plus(15, ChronoUnit.MINUTES);
    Instant expiresAt = (Instant) accessTokenClaims.get("exp");
    assertNotNull(expiresAt, "Expiration claim should be present");
    assertTrue(expiresAt.isBefore(expirationTime) && expiresAt.isAfter(now),
        "Token expiration time should be within 15 minutes from now");

    Map<String, Object> refreshTokenClaims = jwtService.decodeJwt(tokens.get("refreshToken"));
    assertNotNull(refreshTokenClaims, "Refresh token claims should not be null");

    assertEquals(username, refreshTokenClaims.get("sub"),
        "Subject claim should match the username");

    assertEquals("self", refreshTokenClaims.get("iss"), "Issuer claim should be 'self'");


    now = Instant.now();
    expirationTime = now.plus(7, ChronoUnit.DAYS);
    expiresAt = (Instant) refreshTokenClaims.get("exp");
    assertNotNull(expiresAt, "Expiration claim should be present");
    assertTrue(expiresAt.isBefore(expirationTime) && expiresAt.isAfter(now),
        "Token expiration time should be within 7 days from now");
  }

  /**
   * Tests {@code decodeJwt()} to ensure that a JWT token is correctly decoded
   * and the returned claims map contains expected values such as the subject, issuer,
   * roles, and expiration time.
   */
  @Test
  void givenValidJwtTokenWhenDecodeJwtThenReturnMapOfClaims() {
    String token = jwtService.generateAccessToken(auth);
    Instant now = Instant.now();
    Map<String, Object> claimsMap = Map.of(
        "sub", username,
        "iss", "self",
        "roles", "USER",
        "iat", now,
        "exp", now.plus(1, ChronoUnit.HOURS)
    );

    Map<String, Object> result = jwtService.decodeJwt(token);

    assertNotNull(result, "Decoded claims should not be null");
    assertEquals(claimsMap.get("sub"), result.get("sub"),
        "The 'sub' claim should match the expected username");

    assertEquals(claimsMap.get("iss"), result.get("iss"),
        "The 'iss' claim should be 'self'");

    assertEquals(claimsMap.get("roles"), result.get("roles"),
        "The 'roles' claim should match the authority");

    now = Instant.now();
    Instant expirationTime = (Instant) claimsMap.get("exp");
    Instant expiresAt = (Instant) result.get("exp");
    assertNotNull(expiresAt, "Expiration claim should be present");
    assertTrue(expiresAt.isBefore(expirationTime) && expiresAt.isAfter(now),
        "Token expiration time should be within 1 hour from now");
  }

  /**
   * Tests {@code jwtAuthenticationConverter()} bean configuration to verify that
   * {@link JwtGrantedAuthoritiesConverter} prefixes roles with "ROLE_" and extracts
   * authorities correctly from the "roles" claim in the JWT token.
   */
  @Test
  void givenJwtAuthenticationConverterBeanWhenJwtAuthenticationConverterThenVerifyConfiguration() {
    JwtAuthenticationConverter jwtAuthenticationConverter =
        jwtService.jwtAuthenticationConverter();
    assertNotNull(jwtAuthenticationConverter);

    Jwt jwt = new Jwt(
        "mockTokenValue",
        Instant.now(),
        Instant.now().plus(1, ChronoUnit.HOURS),
        Map.of("alg", "none"),
        Map.of("sub", "testUser", "roles", List.of("USER", "ADMIN"))
    );

    Collection<GrantedAuthority> authorities =
        jwtAuthenticationConverter.convert(jwt).getAuthorities();

    assertEquals(2, authorities.size(), "Should contain two authorities");
    assertEquals(
        "ROLE_USER",
        authorities.stream().findFirst().get().getAuthority(),
        "First role should be prefixed with 'ROLE_'");
    assertEquals(
        "ROLE_ADMIN",
        authorities.stream().skip(1).findFirst().get().getAuthority(),
        "Second role should be prefixed with 'ROLE_'");
  }
}
