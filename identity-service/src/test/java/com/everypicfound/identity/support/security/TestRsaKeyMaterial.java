package com.everypicfound.identity.support.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class TestRsaKeyMaterial {

    private TestRsaKeyMaterial() {
    }

    public static KeyPair generate(int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA algorithm is unavailable", exception);
        }
    }

    public static Path writePrivateKey(Path directory, String filename, KeyPair keyPair) {
        return writePem(directory, filename, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    public static Path writePublicKey(Path directory, String filename, KeyPair keyPair) {
        return writePem(directory, filename, "PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    private static Path writePem(Path directory, String filename, String label, byte[] derBytes) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(derBytes);
        String pem = "-----BEGIN " + label + "-----\n"
                + encoded
                + "\n-----END " + label + "-----\n";
        Path target = directory.resolve(filename);
        try {
            return Files.writeString(target, pem, StandardCharsets.US_ASCII);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write test PEM file", exception);
        }
    }
}
