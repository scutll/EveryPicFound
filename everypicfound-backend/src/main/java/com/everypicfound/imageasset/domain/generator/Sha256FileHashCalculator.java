package com.everypicfound.imageasset.domain.generator;

import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
public class Sha256FileHashCalculator implements FileHashCalculator {
    
    @Override
    public String calculateHash(InputStream inputStream) {
        throw new UnsupportedOperationException("TODO");
    }
}
