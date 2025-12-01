-- PUBLIC
-- Typ dynamic_dto: Umożliwia obsługę polimorficznych danych w PostgreSQL.
--
-- CEL: Służy jako uniwersalny kontener do przesyłania dynamicznych struktur danych,
-- których typ jest określany w czasie wykonania zapytania, a nie w schemacie bazy.
--
-- STRUKTURA:
--   - type_name: Klucz (np. "profile_dto"), który framework po stronie aplikacji
--                mapuje na konkretną klasę (np. DynamicProfile).
--   - data_payload: Właściwe dane w formacie JSONB.
--
-- UŻYCIE:
-- Idealny do agregacji różnych typów danych w jednej kolumnie, np. w JOIN LATERAL
-- lub w tablicach przechowujących heterogeniczne obiekty.
-- Dla statycznych, dobrze zdefiniowanych struktur danych, preferowane jest użycie
-- dedykowanego typu kompozytowego (CREATE TYPE my_static_type AS (...)).
CREATE TYPE dynamic_dto AS (
    type_name text,
    data_payload jsonb
);

CREATE OR REPLACE FUNCTION dynamic_dto(p_type_name TEXT, p_data JSONB)
    RETURNS dynamic_dto AS $$
BEGIN
    RETURN ROW(p_type_name, p_data)::dynamic_dto;
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

-- Ta nazwa jest zawsze prawdziwa. Tworzy DTO z dowolnej wartości, którą da się rzutować na JSONB.
CREATE OR REPLACE FUNCTION to_dynamic_dto(p_type_name TEXT, p_value ANYELEMENT)
    RETURNS dynamic_dto AS $$
BEGIN
    RETURN ROW(p_type_name, to_jsonb(p_value))::dynamic_dto;
END;
$$ LANGUAGE plpgsql;

-- Przeciążenie dla TEXT
CREATE OR REPLACE FUNCTION to_dynamic_dto(p_type_name TEXT, p_value TEXT)
    RETURNS dynamic_dto AS $$
BEGIN
    RETURN ROW(p_type_name, to_jsonb(p_value))::dynamic_dto;
END;
$$ LANGUAGE plpgsql;

-- Funkcja do odpakowywania
CREATE OR REPLACE FUNCTION unwrap_dto_payload(p_dto dynamic_dto)
    RETURNS JSONB AS $$
BEGIN
    RETURN p_dto.data_payload;
END;
$$ LANGUAGE plpgsql;

---------- TRIGGER

CREATE FUNCTION public.update_modified_column() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

---------- REPORT CONFIG
CREATE TYPE public.filter_config AS
(
    column_name text,
    config      jsonb
);

CREATE TYPE public.sort_direction AS ENUM (
    'ASCENDING',
    'DESCENDING'
    );

CREATE TYPE public.sort_configuration AS
(
    column_name    text,
    sort_direction public.sort_direction
);

CREATE TABLE public.report_configurations
(
    id              integer                                            NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name            text                                               NOT NULL,
    report_name     text                                               NOT NULL,
    description     text,
    is_default      boolean                                            NOT NULL,
    sort_order      public.sort_configuration[]                        NOT NULL,
    visible_columns text[]                                             NOT NULL,
    column_order    text[]                                             NOT NULL,
    page_size       int8                                               NOT NULL,
    filters         public.filter_config[],
    created_at      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT report_configurations_name_type_unique UNIQUE (name, report_name)
);

CREATE OR REPLACE TRIGGER update_report_configurations_modtime
    BEFORE UPDATE
    ON public.report_configurations
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

---------- API

CREATE TABLE public.api_integrations
(
    id           integer PRIMARY KEY                    NOT NULL GENERATED ALWAYS AS IDENTITY,
    name         text UNIQUE                            NOT NULL,
    enabled      boolean                  DEFAULT false NOT NULL,
    api_key      text,
    endpoint_url text,
    port         integer,
    last_sync    timestamp with time zone,
    created_at   timestamp with time zone DEFAULT now(),
    updated_at   timestamp with time zone DEFAULT now()
);

CREATE TABLE public.application_settings
(
    id       integer PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    language TEXT DEFAULT 'pl'
    -- inne ustawienia UI/aplikacji w przyszłości
);

-- ASIAN MEDIA

---- Home Screen

------ Typ reprezentujący pojedynczy element na liście szybkiego dostępu
CREATE TYPE asian_media.dashboard_item AS
(
    id         INTEGER,
    main_title TEXT
);


---- Typy


CREATE TYPE asian_media.publication_language AS ENUM (
    'KOREAN',
    'CHINESE',
    'JAPANESE'
    );

CREATE TYPE asian_media.publication_type AS ENUM (
    'MANGA',
    'LIGHT_NOVEL',
    'WEB_NOVEL',
    'PUBLISHED_NOVEL',
    'MANHWA',
    'MANHUA',
    'WEBTOON'
    );

CREATE TYPE asian_media.publication_status AS ENUM (
    'NOT_READING',
    'READING',
    'COMPLETED',
    'PLAN_TO_READ'
    );


CREATE FUNCTION asian_media.manage_publication_volumes() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    -- Jeśli track_progress zmienione na TRUE, dodajemy wpis
    IF NEW.track_progress = TRUE AND
       (TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND OLD.track_progress = FALSE)) THEN

        INSERT INTO asian_media.publication_volumes (publication_id)
        VALUES (NEW.id)
        ON CONFLICT (publication_id) DO NOTHING;

        -- Jeśli track_progress zmienione na FALSE, usuwamy wpis
    ELSIF TG_OP = 'UPDATE' AND NEW.track_progress = FALSE AND OLD.track_progress = TRUE THEN
        DELETE FROM asian_media.publication_volumes WHERE publication_id = NEW.id;
    END IF;

    RETURN NEW;
END;
$$;

--------- TABLES

CREATE TABLE IF NOT EXISTS asian_media.titles
(
    id         integer                             NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    titles     text[] COLLATE pg_catalog."default" NOT NULL,
    language   asian_media.publication_language    NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE TRIGGER update_titles_modtime
    BEFORE UPDATE
    ON asian_media.titles
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();


CREATE TABLE asian_media.publications
(
    id               integer PRIMARY KEY                                NOT NULL GENERATED ALWAYS AS IDENTITY,
    title_id         integer                                            NOT NULL,
    publication_type asian_media.publication_type                       NOT NULL,
    status           asian_media.publication_status                     NOT NULL,
    track_progress   boolean                  DEFAULT false             NOT NULL,
    created_at       timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT publications_title_id_publication_type_key UNIQUE (title_id, publication_type),
    CONSTRAINT publications_title_id_fkey FOREIGN KEY (title_id)
        REFERENCES asian_media.titles (id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE OR REPLACE TRIGGER update_publications_modtime
    BEFORE UPDATE
    ON asian_media.publications
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE INDEX IF NOT EXISTS idx_publications_status
    ON asian_media.publications USING btree
        (status ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_publications_title_id
    ON asian_media.publications USING btree
        (title_id ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_publications_track_progress
    ON asian_media.publications USING btree
        (track_progress ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_publications_type
    ON asian_media.publications USING btree
        (publication_type ASC NULLS LAST);


CREATE TABLE asian_media.publication_volumes
(
    publication_id      integer NOT NULL PRIMARY KEY,
    volumes             integer,
    translated_volumes  integer,
    chapters            integer,
    translated_chapters integer,
    original_completed  boolean                  DEFAULT false,
    created_at          timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at          timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT publication_volumes_publication_id_fkey FOREIGN KEY (publication_id)
        REFERENCES asian_media.publications (id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE OR REPLACE TRIGGER update_publication_volumes_modtime
    BEFORE UPDATE
    ON asian_media.publication_volumes
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE OR REPLACE TRIGGER manage_publication_volumes_trigger
    AFTER INSERT OR UPDATE
    ON asian_media.publications
    FOR EACH ROW
EXECUTE FUNCTION asian_media.manage_publication_volumes();

-- GAMES

CREATE TYPE games.game_status AS ENUM (
    'NOT_PLAYING',
    'WITHOUT_THE_END',
    'PLAYED',
    'TO_PLAY',
    'PLAYING'
    );

---------- TABLES
CREATE TABLE IF NOT EXISTS games.series
(
    id   integer PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    name text                NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS games.games
(
    id     integer PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
    name   text                NOT NULL UNIQUE,
    series integer,
    status games.game_status   NOT NULL,
    CONSTRAINT games_series_fkey FOREIGN KEY (series)
        REFERENCES games.series (id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS games.characters
(
    game_id                     integer NOT NULL PRIMARY KEY,
    has_distinctive_character   boolean NOT NULL,
    has_distinctive_protagonist boolean NOT NULL,
    has_distinctive_antagonist  boolean NOT NULL,
    CONSTRAINT game_characters_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.play_time
(
    game_id            integer        NOT NULL PRIMARY KEY,
    completion_count   integer,
    play_time_hours    numeric(10, 2) NOT NULL,
    play_time_interval interval(2), -- aktualnie nieużywane
    CONSTRAINT game_play_time_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.ratings
(
    game_id           integer NOT NULL PRIMARY KEY,
    story_rating      integer,
    gameplay_rating   integer,
    atmosphere_rating integer,
    CONSTRAINT game_ratings_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.categories
(
    id   integer NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name text
);

CREATE TABLE IF NOT EXISTS games.categories_to_games
(
    category_id integer NOT NULL,
    game_id     integer NOT NULL,
    CONSTRAINT categories_to_games_pkey PRIMARY KEY (category_id, game_id),
    CONSTRAINT categories_to_games_category_id_fkey FOREIGN KEY (category_id)
        REFERENCES games.categories (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT categories_to_games_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.time_played_check_time
(
    check_time timestamp without time zone NOT NULL
);

-- game Statistics
-- Typ do przechowywania rozkładu statusów
CREATE TYPE games.dashboard_status_count AS
(
    status games.game_status,
    count  int8
);

-- Typ do przechowywania gry w rankingu wg czasu gry
CREATE TYPE games.dashboard_game_by_time AS
(
    id              integer,
    name            text,
    play_time_hours numeric(10, 2)
);

-- Typ do przechowywania gry w rankingu wg ocen
CREATE TYPE games.dashboard_game_by_rating AS
(
    id             integer,
    name           text,
    average_rating numeric(10, 2)
);

CREATE OR REPLACE VIEW games.time_played
AS
SELECT sum(play_time_hours) AS time_played
FROM games.play_time;
