package io.github.ajmiller611.usermanagement.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * Utility class for generating RSA key pairs.
 *
 * <p>This class provides a method to generate a new RSA key pair with a key size of 2048 bits.
 * It uses the {@link KeyPairGenerator} class to generate the key pair, which can be used
 * for encryption, decryption, or signing operations.
 * </p>
 */
public class KeyGeneratorUtility {

  /**
   * Private constructor for the Utility class to throw an exception if instantiation is attempted.
   */
  private KeyGeneratorUtility() {
    throw new IllegalStateException("Utility class should not be instantiated");
  }

  /**
   * Generates a new RSA key pair with a key size of 2048 bits.
   *
   * <p>This method initializes a {@link KeyPairGenerator} with the RSA algorithm and a key size of
   * 2048 bits. It then generates a new RSA key pair containing a public key and a private key.
   * If an error occurs during key generation, an {@link IllegalStateException} is thrown.
   * </p>
   *
   * @return the generated RSA key pair, including a public key and a private key
   * @throws IllegalStateException if key generation fails
   */
  public static KeyPair generateRsaKey() {

    KeyPair keyPair;

    try {
      // Initialize the key pair generator with the RSA algorithm and a key size of 2048 bits
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);

      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception e) {
      throw new IllegalStateException();
    }
    return keyPair;
  }
}
