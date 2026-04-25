package io.github.ajmiller611.usermanagement.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.ajmiller611.usermanagement.config.RsaKeyProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for generating JWT tokens.
 *
 * <p>This class uses JwtEncoder to encode the JWT claims and generate the final token.
 * It requires an Authentication object to extract user roles and other necessary details.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TokenService {

  private final RsaKeyProperties keys;

  /**
   * Creates a {@link JwtEncoder} bean for encoding JWT tokens using the RSA key pair.
   *
   * <p>This method creates a {@link JwtEncoder} that encodes JWT tokens using the RSA key pair,
   * allowing for the signing of the tokens with the private key and verification using the public
   * key.
   * </p>
   *
   * @return a {@link JwtEncoder} configured with the RSA key pair for JWT encoding
   */
  public JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(keys.getPublicKey()).privateKey(keys.getPrivateKey()).build();
    JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwks);
  }

  /**
   * Creates a {@link JwtDecoder} bean for decoding JWT tokens using the public RSA key.
   *
   * <p>This method creates a {@link JwtDecoder} that decodes JWT tokens using the public key
   * provided in the RSA key properties. The decoder will validate the token's signature using
   * the public key.
   * </p>
   *
   * @return a {@link JwtDecoder} configured with the public key for JWT validation
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(keys.getPublicKey()).build();
  }

  /**
   * Generates a JWT based on the provided authentication object.
   *
   * @param auth the authentication object containing user details and roles
   * @return a signed JWT token as a string
   */
  public String generateAccessToken(Authentication auth) {
    Instant now = Instant.now();
    Instant expirationTime =  now.plus(15, ChronoUnit.MINUTES);

    // Collecting all authorities (roles) granted to the user into a single string
    String scope = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(" "));

    // Build the JWT claims set with essential information
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .expiresAt(expirationTime)
        .subject(auth.getName())
        .claim("roles", scope) // Custom claim to store user roles
        .build();

    // Encode the claims and return the JWT token as a string
    return jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Generates a refresh token JWT (JSON Web Token) for the authenticated user.
   *
   * <p>The refresh token is issued with a longer expiration time (e.g., 7 days)
   * and contains claims such as the issuer, issue time, subject (user), and expiration time.
   * The refresh token is then encoded and returned as a string.
   * </p>
   *
   * @param auth The Authentication object containing the authenticated user's details,
   *             such as the username (subject).
   * @return A string representing the generated refresh token JWT.
   */
  public String generateRefreshToken(Authentication auth) {
    Instant now = Instant.now();

    // Set a longer expiration time for the refresh token (e.g., 7 days)
    Instant expirationTime = now.plus(7, ChronoUnit.DAYS);

    // Build the JWT claims set for the refresh token
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(now)
        .subject(auth.getName())
        .expiresAt(expirationTime)
        .build();

    // Encode the refresh token and return it
    return jwtEncoder().encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  /**
   * Generates both an access token and a refresh token for the authenticated user.
   *
   * <p>This method generates an access token and a refresh token by calling the respective methods
   * for token generation and returns both tokens in a map. The map contains the access token
   * and refresh token as key-value pairs.
   * </p>
   *
   * @param auth The Authentication object containing the authenticated user's details,
   *             such as the username (subject).
   * @return A map containing the generated access token and refresh token,
   *     with the keys "accessToken" and "refreshToken".
   */
  public Map<String, String> generateTokens(Authentication auth) {
    String accessToken = generateAccessToken(auth); // Generate access token
    String refreshToken = generateRefreshToken(auth); // Generate refresh token

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", accessToken);
    tokens.put("refreshToken", refreshToken);

    return tokens;
  }

  /**
   * Decodes the provided JWT token and extracts its claims.
   *
   * <p>This method uses the {@link JwtDecoder} to decode the given token and retrieves the claims
   * stored within it. The claims are returned as a map, where each key represents a claim name
   * and each value is the corresponding claim's value.
   * </p>
   *
   * @param token the JWT token to decode
   * @return a map of claims contained in the JWT
   */
  public Map<String, Object> decodeJwt(String token) {
    // Decode the JWT token and extract its claims.
    Jwt jwt = jwtDecoder().decode(token);

    // Return the claims.
    return jwt.getClaims();
  }

  /**
   * Configures the {@link JwtAuthenticationConverter} to extract roles from JWT claims.
   *
   * <p>This method sets up a {@link JwtAuthenticationConverter} to convert the JWT claims into
   * {@link GrantedAuthority} objects. It specifies the claim name to extract the roles from the JWT
   * and applies the "ROLE_" prefix to the roles for Spring Security.
   * </p>
   *
   * @return a configured {@link JwtAuthenticationConverter} for extracting roles from JWT claims
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthoritiesClaimName("roles");
    converter.setAuthorityPrefix("ROLE_"); // Prefix roles with "ROLE_"
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
    return jwtAuthenticationConverter;
  }
}
