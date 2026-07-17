package com.everypicfound.security.contract.jwt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.Resource;

public final class PemRsaPublicKeyLoader {

    public RSAPublicKey load(Resource resource) {
        try {
            byte[] der = readPem(resource);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException exception) {
            throw new JwtKeyLoadingException("Could not read configured RSA public key resource", exception);
        } catch (GeneralSecurityException | ClassCastException exception) {
            throw new JwtKeyLoadingException("Configured RSA public key is invalid", exception);
        }
    }

    private static byte[] readPem(Resource resource) throws IOException {
        String pem;
        try (var input = resource.getInputStream()) {
            pem = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII);
        }
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        if (normalized.isBlank()) {
            throw new JwtKeyLoadingException("Configured PEM must contain a PUBLIC KEY");
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException exception) {
            throw new JwtKeyLoadingException("Configured PUBLIC KEY contains invalid Base64", exception);
        }
    }
}
