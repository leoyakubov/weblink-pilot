package io.weblinkpilot.url.codegen;

import org.springframework.stereotype.Component;

@Component
public class Base62Codec {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    public String encode(long value) {
        if (value == 0L) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        long current = value;
        while (current > 0) {
            int index = (int) (current % ALPHABET.length);
            builder.append(ALPHABET[index]);
            current /= ALPHABET.length;
        }
        return builder.reverse().toString();
    }
}
