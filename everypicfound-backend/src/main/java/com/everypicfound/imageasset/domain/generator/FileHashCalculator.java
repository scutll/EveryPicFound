package com.everypicfound.imageasset.domain.generator;

import java.io.InputStream;

public interface FileHashCalculator {
    
    String calculateHash(InputStream inputStream);
}
