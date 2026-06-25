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
    when(shortLinkRepository.existsByCode("spring-boot")).thenReturn(false);
    when(shortLinkRepository.existsByCode("vue-js")).thenReturn(false);
    when(shortLinkRepository.existsByCode("postgres")).thenReturn(false);
    when(shortLinkRepository.existsByCode("redis")).thenReturn(false);

    service.seedDefaultLinks("User");

    ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
    verify(shortLinkRepository, times(4)).save(captor.capture());

    assertThat(captor.getAllValues())
        .extracting(ShortLink::getCode)
        .containsExactly("spring-boot", "vue-js", "postgres", "redis");
    assertThat(captor.getAllValues())
        .extracting(ShortLink::getOriginalUrl)
        .containsExactly(
            "https://spring.io/projects/spring-boot",
            "https://vuejs.org/guide/introduction.html",
            "https://www.postgresql.org/about/",
            "https://redis.io/docs/latest/develop/");
    assertThat(captor.getAllValues())
        .extracting(ShortLink::getOwnerUsername)
        .containsExactly(null, null, "user", "user");
  }
}
