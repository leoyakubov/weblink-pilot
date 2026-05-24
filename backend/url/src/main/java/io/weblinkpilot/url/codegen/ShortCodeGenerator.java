package io.weblinkpilot.url.codegen;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int DEFAULT_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}