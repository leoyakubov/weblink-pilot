package io.weblinkpilot.ai.provider;

import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StubAiProvider implements AiProvider {

  private static final Pattern NON_ALIAS_CHARS = Pattern.compile("[^a-z0-9]+");

  @Override
  public String name() {
    return "stub";
  }

  @Override
  public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
    URI uri = URI.create(prompt.originalUrl());
    String host = uri.getHost() == null ? prompt.originalUrl() : uri.getHost();
    String readableHost = host.replaceFirst("^www\\.", "");
    String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
    String lastSegment = lastPathSegment(path);
    String title = titleFrom(readableHost, lastSegment);
    String category = categoryFrom(readableHost, path);
    List<String> tags = tagsFrom(readableHost, path, category);
    String alias =
        prompt.customAlias() == null ? aliasFrom(lastSegment, readableHost) : prompt.customAlias();
    return new AiLinkMetadataResult(
        title,
        "A clean short-link preview for "
            + readableHost
            + " that helps people understand the destination before opening it.",
        category,
        tags,
        iconFor(category),
        alias);
  }

  private String lastPathSegment(String path) {
    String[] parts = path.split("/");
    for (int index = parts.length - 1; index >= 0; index--) {
      if (!parts[index].isBlank()) {
        return parts[index];
      }
    }
    return "home";
  }

  private String titleFrom(String host, String lastSegment) {
    String base = "home".equals(lastSegment) ? host : lastSegment;
    String normalized = NON_ALIAS_CHARS.matcher(base.toLowerCase(Locale.ROOT)).replaceAll(" ");
    StringBuilder title = new StringBuilder();
    for (String word : normalized.trim().split("\\s+")) {
      if (word.isBlank()) {
        continue;
      }
      if (!title.isEmpty()) {
        title.append(' ');
      }
      title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return title.isEmpty() ? host : title.toString();
  }

  private String categoryFrom(String host, String path) {
    String value = (host + " " + path).toLowerCase(Locale.ROOT);
    if (value.contains("spring")
        || value.contains("java")
        || value.contains("vue")
        || value.contains("postgres")
        || value.contains("redis")) {
      return "Programming";
    }
    if (value.contains("docs") || value.contains("guide") || value.contains("reference")) {
      return "Documentation";
    }
    if (value.contains("github")) {
      return "Source code";
    }
    return "Web resource";
  }

  private List<String> tagsFrom(String host, String path, String category) {
    String value = (host + " " + path).toLowerCase(Locale.ROOT);
    if (value.contains("spring")) {
      return List.of("spring", "java", "backend");
    }
    if (value.contains("vue")) {
      return List.of("vue", "frontend", "javascript");
    }
    if (value.contains("postgres")) {
      return List.of("postgres", "database", "sql");
    }
    if (value.contains("redis")) {
      return List.of("redis", "cache", "data");
    }
    if (value.contains("github")) {
      return List.of("github", "source", "project");
    }
    return List.of(category.toLowerCase(Locale.ROOT).replace(" ", "-"), "link");
  }

  private String aliasFrom(String lastSegment, String host) {
    String source = "home".equals(lastSegment) ? host : lastSegment;
    String alias = NON_ALIAS_CHARS.matcher(source.toLowerCase(Locale.ROOT)).replaceAll("-");
    alias = alias.replaceAll("^-+|-+$", "");
    if (alias.length() < 3) {
      alias = "link-" + alias;
    }
    return alias.length() > 64 ? alias.substring(0, 64).replaceAll("-+$", "") : alias;
  }

  private String iconFor(String category) {
    return switch (category) {
      case "Programming" -> "code";
      case "Documentation" -> "docs";
      case "Source code" -> "repo";
      default -> "link";
    };
  }
}
