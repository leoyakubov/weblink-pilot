alter table click_events
    add column if not exists event_source varchar(16) not null default 'REDIRECT';

update click_events
set event_source = coalesce(event_source, 'REDIRECT');

create index if not exists idx_click_events_short_code_source on click_events(short_code, event_source);
