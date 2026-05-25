package io.weblinkpilot.url.codegen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base62CodecTest {

    private final Base62Codec codec = new Base62Codec();

    @Test
    void encodesLongValuesIntoBase62() {
        assertThat(codec.encode(0)).isEqualTo("0");
        assertThat(codec.encode(61)).isEqualTo("z");
        assertThat(codec.encode(62)).isEqualTo("10");
    }
}
