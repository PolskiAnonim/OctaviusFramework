-- Niezależne sloty kategorii - oddzielne od activity_log, własne zakresy czasowe
CREATE TABLE activity_tracker.category_slots (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    category_id integer NOT NULL REFERENCES activity_tracker.categories(id) ON DELETE CASCADE,
    started_at timestamptz NOT NULL,
    ended_at timestamptz NOT NULL,
    source_process_name text  -- wypełniane przy auto-fill z reguł kategoryzacji
);

CREATE INDEX idx_category_slots_started_at ON activity_tracker.category_slots(started_at);
CREATE INDEX idx_category_slots_process ON activity_tracker.category_slots(source_process_name);
