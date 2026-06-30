package io.weblinkpilot.shared.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.weblinkpilot.shared.types.LinkTrackingSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoSeedDataCatalog {

  private static final String RESOURCE_PATH = "/seed/demo-seed-data.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final DemoSeedData DATA = load();

  private DemoSeedDataCatalog() {}

  public static List<DemoLink> links() {
    return DATA.links();
  }

  public static List<String> codes() {
    return DATA.links().stream().map(DemoLink::code).toList();
  }

  public static Map<String, List<DemoAnalyticsEvent>> analyticsEventsByCode() {
    Map<String, List<DemoAnalyticsEvent>> eventsByCode = new LinkedHashMap<>();
    DATA.links().forEach(link -> eventsByCode.put(link.code(), link.analytics()));
    return Collections.unmodifiableMap(eventsByCode);
  }

  private static DemoSeedData load() {
    try (InputStream inputStream = DemoSeedDataCatalog.class.getResourceAsStream(RESOURCE_PATH)) {
      if (inputStream == null) {
        throw new IllegalStateException("Demo seed data resource not found: " + RESOURCE_PATH);
      }
      return OBJECT_MAPPER.readValue(inputStream, DemoSeedData.class);
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not load demo seed data: " + RESOURCE_PATH, exception);
    }
  }

  public enum OwnerKind {
    ANONYMOUS,
    BOOTSTRAP_USER
  }

  public record DemoSeedData(List<DemoLink> links) {
    public DemoSeedData {
      links = links == null ? List.of() : List.copyOf(links);
    }

    @Override
    public List<DemoLink> links() {
      return List.copyOf(links);
    }
  }

  public record DemoLink(
      String code,
      String originalUrl,
      String customAlias,
      OwnerKind ownerKind,
      DemoLinkMetadata metadata,
      List<DemoAnalyticsEvent> analytics) {
    public DemoLink {
      analytics = analytics == null ? List.of() : List.copyOf(analytics);
    }

    @Override
    public List<DemoAnalyticsEvent> analytics() {
      return List.copyOf(analytics);
    }
  }

  public record DemoLinkMetadata(
      String title,
      String summary,
      String category,
      List<String> tags,
      String icon,
      String suggestedAlias) {
    public DemoLinkMetadata {
      tags = tags == null ? List.of() : List.copyOf(tags);
    }

    @Override
    public List<String> tags() {
      return List.copyOf(tags);
    }
  }

  public record DemoAnalyticsEvent(
      int daysAgo,
      int hoursAgo,
      LinkTrackingSource source,
      String ipAddress,
      String country,
      String browserFamily,
      String deviceType,
      String referrer) {}
}
