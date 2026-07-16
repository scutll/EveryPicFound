package com.everypicfound.identity.infrastructure.security.jwt.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everypicfound.identity.support.security.TestRsaKeyMaterial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

class PemRsaKeyLoaderTest {

    @TempDir
    Path tempDirectory;

    private final PemRsaKeyLoader loader = new PemRsaKeyLoader();

    @Test
    void loadsMatchingPkcs8PrivateAndX509PublicKeys() {
        KeyPair keyPair = TestRsaKeyMaterial.generate(2048);
        Path privatePem = TestRsaKeyMaterial.writePrivateKey(tempDirectory, "private.pem", keyPair);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "public.pem", keyPair);

        RsaKeyMaterial loaded = loader.load(
                new FileSystemResource(privatePem),
                new FileSystemResource(publicPem));

        assertThat(loaded.privateKey()).isEqualTo(keyPair.getPrivate());
        assertThat(loaded.publicKey()).isEqualTo(keyPair.getPublic());
    }

    @Test
    void rejectsKeysSmallerThan2048Bits() {
        KeyPair weakKeyPair = TestRsaKeyMaterial.generate(1024);
        Path privatePem = TestRsaKeyMaterial.writePrivateKey(tempDirectory, "weak-private.pem", weakKeyPair);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "weak-public.pem", weakKeyPair);

        assertThatThrownBy(() -> loader.load(
                new FileSystemResource(privatePem),
                new FileSystemResource(publicPem)))
                .isInstanceOf(JwtKeyConfigurationException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void rejectsMismatchedPrivateAndPublicKeys() {
        KeyPair privateKeyPair = TestRsaKeyMaterial.generate(2048);
        KeyPair publicKeyPair = TestRsaKeyMaterial.generate(2048);
        Path privatePem = TestRsaKeyMaterial.writePrivateKey(tempDirectory, "private.pem", privateKeyPair);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "other-public.pem", publicKeyPair);

        assertThatThrownBy(() -> loader.load(
                new FileSystemResource(privatePem),
                new FileSystemResource(publicPem)))
                .isInstanceOf(JwtKeyConfigurationException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void rejectsUnexpectedPemLabelsWithoutLeakingPemContent() {
        String secretMarker = "DO_NOT_LEAK_THIS_KEY_CONTENT";
        ByteArrayResource invalidPrivate = new ByteArrayResource((
                "-----BEGIN RSA PRIVATE KEY-----\n" + secretMarker + "\n-----END RSA PRIVATE KEY-----")
                .getBytes(StandardCharsets.US_ASCII));
        KeyPair keyPair = TestRsaKeyMaterial.generate(2048);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "public.pem", keyPair);

        assertThatThrownBy(() -> loader.load(invalidPrivate, new FileSystemResource(publicPem)))
                .isInstanceOf(JwtKeyConfigurationException.class)
                .hasMessageContaining("PKCS#8")
                .hasMessageNotContaining(secretMarker);
    }

    @Test
    void rejectsMalformedBase64WithoutLeakingItsContent() {
        String invalidContent = "NOT_VALID_BASE64_SECRET";
        ByteArrayResource invalidPrivate = new ByteArrayResource((
                "-----BEGIN PRIVATE KEY-----\n" + invalidContent + "\n-----END PRIVATE KEY-----")
                .getBytes(StandardCharsets.US_ASCII));
        KeyPair keyPair = TestRsaKeyMaterial.generate(2048);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "public.pem", keyPair);

        assertThatThrownBy(() -> loader.load(invalidPrivate, new FileSystemResource(publicPem)))
                .isInstanceOf(JwtKeyConfigurationException.class)
                .hasMessageContaining("Base64")
                .hasMessageNotContaining(invalidContent);
    }

    @Test
    void rejectsNonRsaKeyMaterial() throws Exception {
        KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance("EC");
        ecGenerator.initialize(256);
        KeyPair ecKeyPair = ecGenerator.generateKeyPair();
        Path privatePem = TestRsaKeyMaterial.writePrivateKey(tempDirectory, "ec-private.pem", ecKeyPair);
        Path publicPem = TestRsaKeyMaterial.writePublicKey(tempDirectory, "ec-public.pem", ecKeyPair);

        assertThatThrownBy(() -> loader.load(
                new FileSystemResource(privatePem),
                new FileSystemResource(publicPem)))
                .isInstanceOf(JwtKeyConfigurationException.class)
                .hasMessageContaining("valid RSA");
    }
}
