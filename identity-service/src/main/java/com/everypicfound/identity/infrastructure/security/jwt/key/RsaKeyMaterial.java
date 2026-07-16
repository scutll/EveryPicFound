package com.everypicfound.identity.infrastructure.security.jwt.key;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

public record RsaKeyMaterial(RSAPrivateKey privateKey, RSAPublicKey publicKey) {

    public RsaKeyMaterial {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(publicKey, "publicKey must not be null");
    }
}
