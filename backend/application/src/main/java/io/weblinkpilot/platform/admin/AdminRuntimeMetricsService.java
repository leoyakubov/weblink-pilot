package io.weblinkpilot.platform.admin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.weblinkpilot.platform.observability.BusinessMetric;
import io.weblinkpilot.shared.api.admin.AdminRuntimeMetricResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "MeterRegistry is a Spring-managed metrics collaborator retained by this service.")
public class AdminRuntimeMetricsService {

  private static final String HEAP_TAG_NAME = "area";
  private static final String HEAP_TAG_VALUE = "heap";
  private static final String NOT_AVAILABLE = "N/A";

  private final MeterRegistry meterRegistry;

  public AdminRuntimeMetricsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public List<AdminRuntimeMetricResponse> metrics() {
    List<AdminRuntimeMetricResponse> metrics = new ArrayList<>();
    for (RuntimeMetric metric : RuntimeMetric.values()) {
      metrics.add(metric.response(value(metric)));
    }
    return metrics;
  }

  private String value(RuntimeMetric metric) {
    return switch (metric.measure()) {
      case COUNTER -> number(counter(metric.meterName()));
      case GAUGE -> number(gauge(metric.meterName()));
      case HEAP_GAUGE_SUM -> bytes(gaugeSum(metric.meterName(), HEAP_TAG_NAME, HEAP_TAG_VALUE));
      case TIMER_COUNT -> number(timerCount(metric.meterName()));
      case TIMER_TOTAL_MILLIS -> millis(timerTotal(metric.meterName()));
    };
  }

  private double gauge(String name) {
    Gauge gauge = meterRegistry.find(name).gauge();
    return gauge == null ? Double.NaN : gauge.value();
  }

  private double gaugeSum(String name, String tagName, String tagValue) {
    return meterRegistry.find(name).tag(tagName, tagValue).gauges().stream()
        .mapToDouble(Gauge::value)
        .sum();
  }

  private double counter(String name) {
    Counter counter = meterRegistry.find(name).counter();
    return counter == null ? Double.NaN : counter.count();
  }

  private long timerCount(String name) {
    Timer timer = meterRegistry.find(name).timer();
    return timer == null ? 0L : timer.count();
  }

  private double timerTotal(String name) {
    Timer timer = meterRegistry.find(name).timer();
    return timer == null ? Double.NaN : timer.totalTime(TimeUnit.MILLISECONDS);
  }

  private String number(double value) {
    if (Double.isNaN(value)) {
      return NOT_AVAILABLE;
    }
    return String.format(Locale.ROOT, "%.0f", value);
  }

  private String number(long value) {
    return Long.toString(value);
  }

  private String bytes(double value) {
    if (Double.isNaN(value) || value <= 0) {
      return NOT_AVAILABLE;
    }
    return String.format(Locale.ROOT, "%.1f MB", value / 1024 / 1024);
  }

  private String millis(double value) {
    if (Double.isNaN(value)) {
      return NOT_AVAILABLE;
    }
    return String.format(Locale.ROOT, "%.0f", value);
  }

  private enum RuntimeMetricGroup {
    JVM_MEMORY("JVM memory"),
    THREADS("Threads"),
    GARBAGE_COLLECTION("Garbage collection"),
    HTTP("HTTP"),
    CACHE("Cache"),
    DATASOURCE("Datasource"),
    SERVICE_COUNTERS("Service counters");

    private final String displayName;

    RuntimeMetricGroup(String displayName) {
      this.displayName = displayName;
    }

    private String displayName() {
      return displayName;
    }
  }

  private enum RuntimeMetricMeasure {
    COUNTER,
    GAUGE,
    HEAP_GAUGE_SUM,
    TIMER_COUNT,
    TIMER_TOTAL_MILLIS
  }

  private enum RuntimeMetric {
    HEAP_USED(
        RuntimeMetricGroup.JVM_MEMORY,
        "Heap used",
        "jvm.memory.used",
        "bytes",
        "Current heap memory used.",
        RuntimeMetricMeasure.HEAP_GAUGE_SUM),
    HEAP_MAX(
        RuntimeMetricGroup.JVM_MEMORY,
        "Heap max",
        "jvm.memory.max",
        "bytes",
        "Maximum heap memory available.",
        RuntimeMetricMeasure.HEAP_GAUGE_SUM),
    LIVE_THREADS(
        RuntimeMetricGroup.THREADS,
        "Live threads",
        "jvm.threads.live",
        "threads",
        "Currently live JVM threads.",
        RuntimeMetricMeasure.GAUGE),
    PEAK_THREADS(
        RuntimeMetricGroup.THREADS,
        "Peak threads",
        "jvm.threads.peak",
        "threads",
        "Peak JVM thread count.",
        RuntimeMetricMeasure.GAUGE),
    GC_PAUSE_COUNT(
        RuntimeMetricGroup.GARBAGE_COLLECTION,
        "GC pause count",
        "jvm.gc.pause",
        "events",
        "Observed GC pauses.",
        RuntimeMetricMeasure.TIMER_COUNT),
    GC_PAUSE_TIME(
        RuntimeMetricGroup.GARBAGE_COLLECTION,
        "GC pause time",
        "jvm.gc.pause",
        "ms",
        "Total observed GC pause time.",
        RuntimeMetricMeasure.TIMER_TOTAL_MILLIS),
    HTTP_REQUESTS(
        RuntimeMetricGroup.HTTP,
        "Requests",
        "http.server.requests",
        "requests",
        "HTTP requests observed by Micrometer.",
        RuntimeMetricMeasure.TIMER_COUNT),
    HTTP_REQUEST_TIME(
        RuntimeMetricGroup.HTTP,
        "Request time",
        "http.server.requests",
        "ms",
        "Total HTTP request time.",
        RuntimeMetricMeasure.TIMER_TOTAL_MILLIS),
    CACHE_GETS(
        RuntimeMetricGroup.CACHE,
        "Cache gets",
        "cache.gets",
        "ops",
        "Cache lookup operations.",
        RuntimeMetricMeasure.COUNTER),
    CACHE_PUTS(
        RuntimeMetricGroup.CACHE,
        "Cache puts",
        "cache.puts",
        "ops",
        "Cache write operations.",
        RuntimeMetricMeasure.COUNTER),
    ACTIVE_CONNECTIONS(
        RuntimeMetricGroup.DATASOURCE,
        "Active connections",
        "hikaricp.connections.active",
        "connections",
        "Active datasource connections.",
        RuntimeMetricMeasure.GAUGE),
    IDLE_CONNECTIONS(
        RuntimeMetricGroup.DATASOURCE,
        "Idle connections",
        "hikaricp.connections.idle",
        "connections",
        "Idle datasource connections.",
        RuntimeMetricMeasure.GAUGE),
    LINKS_CREATED(
        RuntimeMetricGroup.SERVICE_COUNTERS,
        "Links created",
        BusinessMetric.LINKS_CREATED.meterName(),
        "events",
        "Short-link creation events.",
        RuntimeMetricMeasure.COUNTER),
    REDIRECT_CLICKS(
        RuntimeMetricGroup.SERVICE_COUNTERS,
        "Redirect clicks",
        BusinessMetric.LINKS_CLICKED.meterName(),
        "events",
        "Redirect click events.",
        RuntimeMetricMeasure.COUNTER),
    ANALYTICS_SUMMARIES(
        RuntimeMetricGroup.SERVICE_COUNTERS,
        "Analytics summaries",
        "weblinkpilot.analytics.summary.requests",
        "requests",
        "Analytics summary requests.",
        RuntimeMetricMeasure.COUNTER);

    private final RuntimeMetricGroup group;
    private final String name;
    private final String meterName;
    private final String unit;
    private final String description;
    private final RuntimeMetricMeasure measure;

    RuntimeMetric(
        RuntimeMetricGroup group,
        String name,
        String meterName,
        String unit,
        String description,
        RuntimeMetricMeasure measure) {
      this.group = group;
      this.name = name;
      this.meterName = meterName;
      this.unit = unit;
      this.description = description;
      this.measure = measure;
    }

    private RuntimeMetricMeasure measure() {
      return measure;
    }

    private String meterName() {
      return meterName;
    }

    private AdminRuntimeMetricResponse response(String value) {
      return new AdminRuntimeMetricResponse(group.displayName(), name, value, unit, description);
    }
  }
}
