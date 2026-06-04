package io.github.ajmiller611.usermanagement.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating secure, URL-safe invitation tokens.
 *
 * <p>This service uses {@link SecureRandom} to generate cryptographically secure
 * random bytes, which are then encoded into URL-safe text using Base64 encoding.</p>
 */
@Service
public class InvitationTokenService {

  private final SecureRandom random = new SecureRandom();

  /**
   * Generates a secure random invitation token that is safe to use in URLs.
   *
   * @return a URL-safe invitation token string
   */
  public String generateInvitationToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
