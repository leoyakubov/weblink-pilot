package io.weblinkpilot.shared.api.common;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public PaginatedResponse {
    content = List.copyOf(content);
  }

  public static <T> PaginatedResponse<T> of(
      List<T> content, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
    return new PaginatedResponse<>(
        List.copyOf(content),
        page,
        size,
        totalElements,
        totalPages,
        page <= 0,
        totalPages == 0 || page >= totalPages - 1);
  }

  @Override
  public List<T> content() {
    return List.copyOf(content);
  }
}
