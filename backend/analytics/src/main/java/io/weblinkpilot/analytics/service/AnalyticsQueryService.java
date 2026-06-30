package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.api.analytics.AnalyticsBreakdownStatResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsBucketStatResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsDetailsResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsEventResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.cache.CacheNames;
import io.weblinkpilot.shared.types.LinkTrackingSource;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsQueryService {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsQueryService.class);
  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter HOUR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

  private final ClickEventRepository repository;

  public AnalyticsQueryService(ClickEventRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheNames.ANALYTICS_COUNTS, key = "#code")
  public long countClicks(String code) {
    long count = repository.countByShortCode(code);
    log.info("analytics.query.count code={} count={}", code, count);
    return count;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheNames.ANALYTICS_SUMMARIES, key = "#code")
  public AnalyticsSummaryResponse summarize(String code) {
    long totalClicks = repository.countByShortCode(code);
    long redirectClicks =
        repository.countByShortCodeAndEventSource(code, LinkTrackingSource.REDIRECT);
    long qrScans = repository.countByShortCodeAndEventSource(code, LinkTrackingSource.QR_SCAN);
    long uniqueVisitors = repository.countDistinctIpAddressByShortCode(code);
    Optional<ClickEvent> latestEvent = repository.findFirstByShortCodeOrderByClickedAtDesc(code);
    List<AnalyticsCountryStatResponse> topCountries =
        repository.findTopCountriesByShortCode(code).stream()
            .limit(5)
            .map(view -> new AnalyticsCountryStatResponse(view.getCountry(), view.getClicks()))
            .toList();

    ClickEvent latest = latestEvent.orElse(null);
    AnalyticsSummaryResponse summary =
        new AnalyticsSummaryResponse(
            code,
            totalClicks,
            redirectClicks,
            qrScans,
            uniqueVisitors,
            latest == null ? null : latest.getClickedAt(),
            latest == null ? null : latest.getReferrer(),
            latest == null ? null : latest.getBrowserFamily(),
            latest == null ? null : latest.getDeviceType(),
            topCountries);

    log.info(
        "analytics.query.summary code={} totalClicks={} redirectClicks={} qrScans={} uniqueVisitors={} topCountries={}",
        code,
        totalClicks,
        redirectClicks,
        qrScans,
        uniqueVisitors,
        topCountries.size());
    return summary;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheNames.ANALYTICS_DETAILS, key = "#code")
  public AnalyticsDetailsResponse details(String code) {
    List<ClickEvent> events = repository.findByShortCodeOrderByClickedAtAsc(code);

    AnalyticsDetailsResponse response =
        new AnalyticsDetailsResponse(
            code,
            bucketize(events, Bucket.DAY),
            bucketize(events, Bucket.HOUR),
            breakdown(events, event -> label(event.getBrowserFamily(), "UNKNOWN"), 8),
            breakdown(events, event -> label(event.getDeviceType(), "UNKNOWN"), 8),
            breakdown(events, event -> referrerLabel(event.getReferrer()), 8),
            recentEvents(events, 20),
            bucketize(events, Bucket.DAY),
            bucketize(events, Bucket.DAY));

    log.info(
        "analytics.query.details code={} events={} browsers={} devices={} referrers={}",
        code,
        events.size(),
        response.browserBreakdown().size(),
        response.deviceBreakdown().size(),
        response.referrerBreakdown().size());
    return response;
  }

  private List<AnalyticsBucketStatResponse> bucketize(List<ClickEvent> events, Bucket bucket) {
    Map<String, BucketAccumulator> buckets = new TreeMap<>();
    for (ClickEvent event : events) {
      String key = bucketKey(event, bucket);
      buckets.computeIfAbsent(key, ignored -> new BucketAccumulator()).add(event);
    }

    return buckets.entrySet().stream()
        .map(entry -> entry.getValue().toResponse(entry.getKey()))
        .toList();
  }

  private String bucketKey(ClickEvent event, Bucket bucket) {
    if (event.getClickedAt() == null) {
      return "Unknown";
    }
    return bucket == Bucket.HOUR
        ? HOUR_FORMATTER.format(event.getClickedAt())
        : DAY_FORMATTER.format(event.getClickedAt());
  }

  private List<AnalyticsBreakdownStatResponse> breakdown(
      List<ClickEvent> events, LabelExtractor extractor, int limit) {
    Map<String, Long> counts =
        events.stream()
            .collect(
                Collectors.groupingBy(extractor::label, LinkedHashMap::new, Collectors.counting()));

    return counts.entrySet().stream()
        .sorted(
            Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(limit)
        .map(entry -> new AnalyticsBreakdownStatResponse(entry.getKey(), entry.getValue()))
        .toList();
  }

  private List<AnalyticsEventResponse> recentEvents(List<ClickEvent> events, int limit) {
    List<ClickEvent> newestFirst = new ArrayList<>(events);
    newestFirst.sort(
        Comparator.comparing(
                ClickEvent::getClickedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed());
    return newestFirst.stream()
        .limit(limit)
        .map(
            event ->
                new AnalyticsEventResponse(
                    event.getClickedAt(),
                    event.getEventSource(),
                    event.getReferrer(),
                    event.getCountry(),
                    event.getBrowserFamily(),
                    event.getDeviceType()))
        .toList();
  }

  private String label(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
  }

  private String referrerLabel(String referrer) {
    if (referrer == null || referrer.isBlank()) {
      return "DIRECT / NONE";
    }

    try {
      String host = URI.create(referrer).getHost();
      return host == null || host.isBlank() ? referrer : host;
    } catch (IllegalArgumentException ex) {
      return referrer;
    }
  }

  private enum Bucket {
    DAY,
    HOUR
  }

  @FunctionalInterface
  private interface LabelExtractor {
    String label(ClickEvent event);
  }

  private static final class BucketAccumulator {
    private long totalClicks;
    private long redirectClicks;
    private long qrScans;
    private final Set<String> uniqueIps = new java.util.HashSet<>();

    void add(ClickEvent event) {
      totalClicks++;
      if (event.getEventSource() == LinkTrackingSource.REDIRECT) {
        redirectClicks++;
      } else if (event.getEventSource() == LinkTrackingSource.QR_SCAN) {
        qrScans++;
      }
      if (event.getIpAddress() != null && !event.getIpAddress().isBlank()) {
        uniqueIps.add(event.getIpAddress());
      }
    }

    AnalyticsBucketStatResponse toResponse(String bucket) {
      return new AnalyticsBucketStatResponse(
          bucket, totalClicks, redirectClicks, qrScans, uniqueIps.size());
    }
  }
}
