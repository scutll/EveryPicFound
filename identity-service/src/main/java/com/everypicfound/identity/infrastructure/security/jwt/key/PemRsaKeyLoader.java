package com.everypicfound.identity.infrastructure.security.jwt.key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import org.springframework.core.io.Resource;

/**
 * 从外部 PEM 资源加载并校验一对 RSA 密钥。
 */
public final class PemRsaKeyLoader {

    private static final int MINIMUM_RSA_BITS = 2048;
    private static final byte[] KEY_PAIR_PROBE =
            "everypicfound-rsa-key-pair-check".getBytes(StandardCharsets.US_ASCII);

    public RsaKeyMaterial load(Resource privateKeyResource, Resource publicKeyResource) {
        Objects.requireNonNull(privateKeyResource, "privateKeyResource must not be null");
        Objects.requireNonNull(publicKeyResource, "publicKeyResource must not be null");
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(privateKeyResource, "PRIVATE KEY", "PKCS#8 private key")));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(readPem(publicKeyResource, "PUBLIC KEY", "X.509 public key")));
            validateStrength(privateKey, publicKey);
            validatePair(privateKey, publicKey);
            return new RsaKeyMaterial(privateKey, publicKey);
        } catch (JwtKeyConfigurationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new JwtKeyConfigurationException("Could not read configured RSA key resource", exception);
        } catch (GeneralSecurityException | ClassCastException exception) {
            throw new JwtKeyConfigurationException(
                    "Configured key material is not a valid RSA PKCS#8 private key and X.509 public key pair",
                    exception);
        }
    }

    private static byte[] readPem(Resource resource, String label, String expectedFormat) throws IOException {
        String pem;
        try (var input = resource.getInputStream()) {
            pem = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
        }
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int beginIndex = pem.indexOf(begin);
        int endIndex = pem.indexOf(end);
        if (beginIndex < 0 || endIndex <= beginIndex) {
            throw new JwtKeyConfigurationException("Configured PEM must contain a " + expectedFormat);
        }
        String encoded = pem.substring(beginIndex + begin.length(), endIndex).replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new JwtKeyConfigurationException("Configured " + expectedFormat + " contains invalid Base64", exception);
        }
    }

    private static void validateStrength(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        if (privateKey.getModulus().bitLength() < MINIMUM_RSA_BITS
                || publicKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
            throw new JwtKeyConfigurationException("RSA keys must be at least 2048 bits");
        }
    }

    private static void validatePair(RSAPrivateKey privateKey, RSAPublicKey publicKey)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(KEY_PAIR_PROBE);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(KEY_PAIR_PROBE);
        if (!verifier.verify(signature)) {
            throw new JwtKeyConfigurationException("Configured RSA private and public keys do not match");
        }
    }
}
