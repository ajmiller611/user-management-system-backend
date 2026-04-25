package io.github.ajmiller611.usermanagement.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import io.github.ajmiller611.usermanagement.util.KeyGeneratorUtility;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for {@link RsaKeyProperties} and {@link KeyGeneratorUtility} classes to validate the
 * behavior of RSA key generation and key property initialization.
 *
 * <p>These tests ensure that RSA key pairs are properly generated and that the public and private
 * keys are non-null and of the correct type. The tests also verify that appropriate exceptions
 * are thrown when key generation fails.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RsaKeyConfigurationTests {

  @InjectMocks private RsaKeyProperties rsaKeyProperties;

  /**
   * Sets up the test environment by instantiating the {@link RsaKeyProperties} object
   * before each test. This ensures a fresh instance is available for testing.
   */
  @BeforeEach
  void setUp() {
    rsaKeyProperties = new RsaKeyProperties();
  }

  /**
   * Test to verify that the {@link RsaKeyProperties} instance has non-null public and private keys
   * upon instantiation, and that these keys are of the expected types {@link RSAPublicKey} and
   * {@link RSAPrivateKey}.
   */
  @Test
  void givenRsaKeyPropertiesWhenInstantiatedThenHasNonNullPublicAndPrivateKeys() {
    assertNotNull(rsaKeyProperties.getPublicKey(), "RSA public key should not be null");
    assertNotNull(rsaKeyProperties.getPrivateKey(), "RSA private key should not be null");

    assertInstanceOf(RSAPublicKey.class, rsaKeyProperties.getPublicKey(),
        "Public key should be instance of RSAPublicKey");
    assertInstanceOf(RSAPrivateKey.class, rsaKeyProperties.getPrivateKey(),
        "Private key should be instance of RSAPrivateKey");
  }

  /**
   * Test to verify that the {@link KeyGeneratorUtility#generateRsaKey} method correctly generates
   * a non-null RSA key pair with both public and private keys that are non-null.
   */
  @Test
  void givenGenerateRsaKeyWhenCalledThenReturnNonNullKeyPair() {
    KeyPair keyPair = KeyGeneratorUtility.generateRsaKey();
    assertNotNull(keyPair, "Generated KeyPair should not be null");
    assertNotNull(keyPair.getPublic(), "Public key should not be null");
    assertNotNull(keyPair.getPrivate(), "Private key should not be null");
  }

  /**
   * Test to verify that {@link KeyGeneratorUtility#generateRsaKey} throws
   * an {@link IllegalStateException} when RSA key generation fails due to a simulated
   * {@link NoSuchAlgorithmException} or {@link InvalidParameterException}.
   */
  @Test
  void givenGenerateRsaKeyWhenKeyGenerationFailsThenThrowIllegalStateException() {
    try (var mockedKeyPairGenerator = mockStatic(KeyPairGenerator.class)) {
      mockedKeyPairGenerator.when(() -> KeyPairGenerator.getInstance("RSA"))
          .thenThrow(new NoSuchAlgorithmException("Simulated failure"));

      assertThrows(IllegalStateException.class, KeyGeneratorUtility::generateRsaKey,
          "Expected IllegalStateException when RSA key generation fails due to "
              + "NoSuchAlgorithmException");

      mockedKeyPairGenerator.when(() ->
              KeyPairGenerator.getInstance("RSA").initialize(2048))
          .thenThrow(new InvalidParameterException("Simulated failure"));

      assertThrows(IllegalStateException.class, KeyGeneratorUtility::generateRsaKey,
          "Expected IllegalStateException when RSA key generation fails due to "
              + "InvalidParameterException");
    }
  }

  /**
   * Test the invocation of the constructor of {@link KeyGeneratorUtility} throws an error.
   */
  @Test
  void constructorInvocationForCoverage() {
    // InvocationTargetException is thrown when the constructor is invoked by reflection
    InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
      Constructor<KeyGeneratorUtility> constructor =
          KeyGeneratorUtility.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      constructor.newInstance();
    });

    /*
     * The IllegalStateException is wrapped inside the InvocationTargetException so checking the
     * cause will find the thrown IllegalStateException.
     */
    assertInstanceOf(IllegalStateException.class, exception.getCause(),
        "Expected cause to be IllegalStateException indicating utility class should not be "
            + "instantiated.");
    assertEquals("Utility class should not be instantiated", exception.getCause().getMessage(),
        "Error message should indicate that the utility class should not be instantiated.");
  }
}
