alter table if exists short_links
    add column if not exists deleted_at timestamp with time zone null;

create index if not exists idx_short_links_deleted_at on short_links(deleted_at);
