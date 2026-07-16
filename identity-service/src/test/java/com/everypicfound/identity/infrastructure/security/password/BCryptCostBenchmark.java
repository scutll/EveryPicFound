package com.everypicfound.identity.infrastructure.security.password;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

/**
 * 仅供显式运行的 BCrypt 本机耗时观测，不属于默认单元测试套件。
 */
class BCryptCostBenchmark {

    private static final String BENCHMARK_PASSWORD =
            "benchmark-only-password";
    private static final int FIRST_STRENGTH = 10;
    private static final int LAST_STRENGTH = 14;
    private static final int SAMPLE_COUNT = 3;
    private static final long TARGET_MILLIS = 1_000L;

    @Test
    void measuresSequentialPasswordVerificationCost() {
        for (int strength = FIRST_STRENGTH;
                strength <= LAST_STRENGTH;
                strength++) {
            BCryptPasswordEncoder encoder =
                    new BCryptPasswordEncoder(strength);
            String encoded = encoder.encode(BENCHMARK_PASSWORD);

            encoder.matches(BENCHMARK_PASSWORD, encoded);

            long[] samples = new long[SAMPLE_COUNT];
            for (int index = 0; index < SAMPLE_COUNT; index++) {
                long startedAt = System.nanoTime();
                boolean matched = encoder.matches(
                        BENCHMARK_PASSWORD,
                        encoded);
                long elapsedNanos = System.nanoTime() - startedAt;
                if (!matched) {
                    throw new IllegalStateException(
                            "benchmark password did not match");
                }
                samples[index] = elapsedNanos / 1_000_000L;
            }

            Arrays.sort(samples);
            long medianMillis = samples[SAMPLE_COUNT / 2];
            System.out.printf(
                    "bcrypt strength=%d samplesMs=%s medianMs=%d%n",
                    strength,
                    Arrays.toString(samples),
                    medianMillis);

            if (medianMillis > TARGET_MILLIS) {
                break;
            }
        }
    }
}
