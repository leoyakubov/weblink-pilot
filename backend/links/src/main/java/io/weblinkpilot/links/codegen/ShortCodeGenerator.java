package io.weblinkpilot.links.codegen;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

  private static final int DEFAULT_LENGTH = 7;
  private static final String INVALID_LENGTH_MESSAGE = "length must be positive";

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    return generate(DEFAULT_LENGTH);
  }

  public String generate(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException(INVALID_LENGTH_MESSAGE);
    }

    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(Base62Alphabet.VALUE.charAt(random.nextInt(Base62Alphabet.BASE)));
    }
    return builder.toString();
  }
}
