package io.weblinkpilot.links.codegen;

import org.springframework.stereotype.Component;

@Component
public class Base62Codec {

  public String encode(long value) {
    if (value == 0L) {
      return String.valueOf(Base62Alphabet.ZERO);
    }

    StringBuilder builder = new StringBuilder();
    long current = value;
    while (current > 0) {
      int index = (int) (current % Base62Alphabet.BASE);
      builder.append(Base62Alphabet.VALUE.charAt(index));
      current /= Base62Alphabet.BASE;
    }
    return builder.reverse().toString();
  }
}
