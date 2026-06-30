package io.weblinkpilot.links.codegen;

final class Base62Alphabet {

  static final String VALUE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  static final int BASE = VALUE.length();
  static final char ZERO = VALUE.charAt(0);

  private Base62Alphabet() {}
}
