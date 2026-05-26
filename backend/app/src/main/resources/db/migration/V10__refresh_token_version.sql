alter table app_refresh_tokens
    add column if not exists version bigint not null default 0;
