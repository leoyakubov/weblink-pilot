package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlBootstrapServiceTest {

  @Mock private ShortLinkRepository shortLinkRepository;

  private UrlBootstrapService service;

  @BeforeEach
  void setUp() {
    service = new UrlBootstrapService(shortLinkRepository);
  }

  @Test
  void seedsDefaultLinksWhenMissing() {
    when(shortLinkRepository.existsByCode("welcome")).thenReturn(false);
    when(shortLinkRepository.existsByCode("docs")).thenReturn(false);
    when(shortLinkRepository.existsByCode("user-home")).thenReturn(false);
    when(shortLinkRepository.existsByCode("admin-home")).thenReturn(false);

    service.seedDefaultLinks("User", "Admin");

    ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
    verify(shortLinkRepository, times(4)).save(captor.capture());

    assertThat(captor.getAllValues())
        .extracting(ShortLink::getCode)
        .containsExactly("welcome", "docs", "user-home", "admin-home");
    assertThat(captor.getAllValues().get(2).getOwnerUsername()).isEqualTo("user");
    assertThat(captor.getAllValues().get(3).getOwnerUsername()).isEqualTo("admin");
  }
}
