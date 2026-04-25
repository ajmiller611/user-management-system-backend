package io.github.ajmiller611.usermanagement.config;

import io.github.ajmiller611.usermanagement.util.KeyGeneratorUtility;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Class that holds the RSA public and private key pair.
 *
 * <p>This component is responsible for generating and storing an RSA public and private key pair
 * that can be used for encryption, decryption, or signing operations.
 * </p>
 */
@Component
@Getter
public class RsaKeyProperties {

  private final RSAPublicKey publicKey;
  private final RSAPrivateKey privateKey;

  /**
   * Constructs a new RSAKeyProperties instance by generating a new RSA key pair.
   *
   * <p>The constructor uses {@link KeyGeneratorUtility} to generate a new RSA key pair,
   * and assigns the public and private keys to the corresponding fields.
   * </p>
   */
  public RsaKeyProperties() {
    KeyPair pair = KeyGeneratorUtility.generateRsaKey();
    this.publicKey = (RSAPublicKey) pair.getPublic();
    this.privateKey = (RSAPrivateKey) pair.getPrivate();
  }
}

