package io.weblinkpilot.auth.notification.template;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
public class AccountNotificationTemplateRenderer {

  private static final String TEMPLATE_PREFIX = "templates/";
  private static final String TEMPLATE_SUFFIX = ".txt";
  private static final String LAYOUT_TEMPLATE = "auth/mail/layout";

  private final TemplateEngine templateEngine;

  public AccountNotificationTemplateRenderer() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix(TEMPLATE_PREFIX);
    templateResolver.setSuffix(TEMPLATE_SUFFIX);
    templateResolver.setTemplateMode(TemplateMode.TEXT);
    templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    templateResolver.setCacheable(true);

    this.templateEngine = new TemplateEngine();
    this.templateEngine.setTemplateResolver(templateResolver);
  }

  public AccountNotificationEmail passwordReset(String link) {
    return new AccountNotificationEmail(
        "Reset your WeblinkPilot password",
        renderWithLayout(
            "auth/mail/password-reset",
            Map.of("link", link),
            "Reset your WeblinkPilot password"));
  }

  public AccountNotificationEmail emailVerification(String link) {
    return new AccountNotificationEmail(
        "Verify your WeblinkPilot email",
        renderWithLayout(
            "auth/mail/email-verification",
            Map.of("link", link),
            "Verify your WeblinkPilot email"));
  }

  private String renderWithLayout(
      String bodyTemplate, Map<String, Object> variables, String previewTitle) {
    String body = render(bodyTemplate, variables);
    return render(LAYOUT_TEMPLATE, Map.of("previewTitle", previewTitle, "body", body)).trim();
  }

  private String render(String template, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process(template, context);
  }
}
