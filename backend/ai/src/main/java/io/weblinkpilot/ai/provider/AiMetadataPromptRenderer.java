package io.weblinkpilot.ai.provider;

import org.springframework.stereotype.Component;

@Component
public class AiMetadataPromptRenderer {

  public String systemPrompt() {
    return String.join(
        System.lineSeparator(),
        "You enrich short links for WeblinkPilot.",
        "Return only valid JSON with these fields:",
        "title, summary, category, tags, icon, suggestedAlias.",
        "Keep values concise and product-friendly.",
        "Do not include markdown or any fields not requested.");
  }

  public String userPrompt(AiLinkMetadataPrompt prompt) {
    return String.join(
        System.lineSeparator(),
        "Link code: " + prompt.code(),
        "Current custom alias: " + blankIfNull(prompt.customAlias()),
        "Target URL: " + prompt.originalUrl());
  }

  public String generationPrompt(AiLinkMetadataPrompt prompt) {
    return String.join(
        System.lineSeparator(),
        systemPrompt(),
        "Rules:",
        "- title: human readable, max 80 characters",
        "- summary: one sentence, max 220 characters",
        "- category: short product-friendly category",
        "- tags: 2 to 5 lowercase tags",
        "- icon: one lowercase word such as link, docs, code, repo, video, product",
        "- suggestedAlias: lowercase URL alias, 3-64 chars, only letters, numbers, dash or underscore",
        "",
        userPrompt(prompt));
  }

  private String blankIfNull(String value) {
    return value == null ? "" : value;
  }
}
