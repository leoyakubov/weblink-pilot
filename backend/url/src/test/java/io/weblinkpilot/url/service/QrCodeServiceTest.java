package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

  @Test
  void generatesPngBytes() {
    QrCodeService service = new QrCodeService();

    byte[] png = service.generatePng("http://localhost:8080/r/demo");

    assertThat(png).isNotEmpty();
    assertThat(png[0]).isEqualTo((byte) 0x89);
    assertThat(png[1]).isEqualTo((byte) 0x50);
    assertThat(png[2]).isEqualTo((byte) 0x4E);
    assertThat(png[3]).isEqualTo((byte) 0x47);
  }
}
