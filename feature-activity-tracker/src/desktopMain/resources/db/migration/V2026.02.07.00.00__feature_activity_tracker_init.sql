CREATE SCHEMA IF NOT EXISTS activity_tracker;

-- Enumy
CREATE TYPE activity_tracker.match_type AS ENUM (
    'PROCESS_NAME', 'WINDOW_TITLE', 'REGEX', 'PATH_CONTAINS'
);

CREATE TYPE activity_tracker.document_type AS ENUM (
    'PDF', 'WORD', 'EXCEL', 'POWERPOINT', 'TEXT', 'CODE', 'IMAGE', 'VIDEO', 'OTHER'
);

-- Kategorie (hierarchiczne)
CREATE TABLE activity_tracker.categories (
    id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name text NOT NULL UNIQUE,
    color varchar(7) NOT NULL DEFAULT '#6366F1',
    icon varchar(50),
    parent_id integer REFERENCES activity_tracker.categories(id) ON DELETE SET NULL
);

-- Reguły kategoryzacji
CREATE TABLE activity_tracker.categorization_rules (
    id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    category_id integer NOT NULL REFERENCES activity_tracker.categories(id) ON DELETE CASCADE,
    match_type activity_tracker.match_type NOT NULL,
    pattern text NOT NULL,
    priority integer NOT NULL DEFAULT 100,
    is_active boolean NOT NULL DEFAULT true,
    UNIQUE (match_type, pattern)
);

-- Activity log
CREATE TABLE activity_tracker.activity_log (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    window_title text NOT NULL,
    process_name text NOT NULL,
    process_id integer NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    duration_seconds integer GENERATED ALWAYS AS (
        EXTRACT(EPOCH FROM (ended_at - started_at))::integer
    ) STORED,
    category_id integer REFERENCES activity_tracker.categories(id) ON DELETE SET NULL
);

-- Dokumenty
CREATE TABLE activity_tracker.documents (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    activity_id bigint REFERENCES activity_tracker.activity_log(id) ON DELETE CASCADE,
    type activity_tracker.document_type NOT NULL,
    path text NOT NULL,
    title text,
    timestamp timestamptz NOT NULL DEFAULT now()
);

-- Tagi dokumentów
CREATE TABLE activity_tracker.document_tags (
    document_id bigint REFERENCES activity_tracker.documents(id) ON DELETE CASCADE,
    tag text NOT NULL,
    PRIMARY KEY (document_id, tag)
);

-- Indeksy
CREATE INDEX idx_activity_log_started_at ON activity_tracker.activity_log(started_at);
CREATE INDEX idx_activity_log_category_id ON activity_tracker.activity_log(category_id);
CREATE INDEX idx_documents_timestamp ON activity_tracker.documents(timestamp);
CREATE INDEX idx_rules_priority ON activity_tracker.categorization_rules(priority DESC);

-- Widoki
CREATE VIEW activity_tracker.daily_summary AS
SELECT
    date_trunc('day', started_at) AS activity_date,
    category_id, c.name, c.color,
    COUNT(*) AS count,
    SUM(duration_seconds) AS total_seconds
FROM activity_tracker.activity_log al
LEFT JOIN activity_tracker.categories c ON al.category_id = c.id
WHERE ended_at IS NOT NULL
GROUP BY 1, 2, 3, 4;
