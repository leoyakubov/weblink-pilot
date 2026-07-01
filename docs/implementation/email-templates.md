# Email Templates

Account notification emails are rendered with Thymeleaf text templates in the `auth` module.

## Location

Templates live under:

```text
backend/auth/src/main/resources/templates/auth/mail/
```

Current templates:

- `layout.txt` - shared plain-text account email wrapper
- `password-reset.txt` - password reset body copy
- `email-verification.txt` - email verification body copy

## Rendering Flow

`AccountNotificationTemplateRenderer` renders the specific body template first and then injects it into the shared `layout.txt` template. The SMTP delivery flow stays in `MailAccountNotificationService`.

## Adding A Template

1. Add a new body template under `templates/auth/mail/`.
2. Add a renderer method that passes required variables to Thymeleaf.
3. Keep shared greeting/signature copy in `layout.txt`.
4. Add a focused renderer test for the subject, important copy, links, and shared layout.

Templates use Thymeleaf text mode. Prefer `[[${variable}]]` for escaped variable output.
