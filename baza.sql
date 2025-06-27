-- ASIAN MEDIA

CREATE TABLE IF NOT EXISTS asian_media.publication_volumes
(
    publication_id integer NOT NULL,
    volumes integer,
    translated_volumes integer,
    chapters integer,
    translated_chapters integer,
    original_completed boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT publication_volumes_pkey PRIMARY KEY (publication_id),
    CONSTRAINT publication_volumes_publication_id_fkey FOREIGN KEY (publication_id)
        REFERENCES asian_media.publications (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE OR REPLACE TRIGGER update_publication_volumes_modtime
    BEFORE UPDATE
    ON asian_media.publication_volumes
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE TABLE IF NOT EXISTS asian_media.publications
(
    id SERIAL,
    title_id integer NOT NULL,
    publication_type asian_media.publication_type NOT NULL,
    status asian_media.publication_status NOT NULL,
    track_progress boolean NOT NULL DEFAULT false,
    created_at time with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at time with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT publications_pkey PRIMARY KEY (id),
    CONSTRAINT publications_title_id_publication_type_key UNIQUE (title_id, publication_type),
    CONSTRAINT publications_title_id_fkey FOREIGN KEY (title_id)
        REFERENCES asian_media.titles (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_publications_status
    ON asian_media.publications USING btree
        (status ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS idx_publications_title_id
    ON asian_media.publications USING btree
        (title_id ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS idx_publications_track_progress
    ON asian_media.publications USING btree
        (track_progress ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS idx_publications_type
    ON asian_media.publications USING btree
        (publication_type ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE OR REPLACE TRIGGER manage_publication_volumes_trigger
    AFTER INSERT OR UPDATE
    ON asian_media.publications
    FOR EACH ROW
EXECUTE FUNCTION asian_media.manage_publication_volumes();

CREATE OR REPLACE TRIGGER update_publications_modtime
    BEFORE UPDATE
    ON asian_media.publications
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE TABLE IF NOT EXISTS asian_media.titles
(
    id SERIAL,
    titles text[] COLLATE pg_catalog."default" NOT NULL,
    language asian_media.publication_language NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT titles_pkey PRIMARY KEY (id)
);

CREATE OR REPLACE TRIGGER update_titles_modtime
    BEFORE UPDATE
    ON asian_media.titles
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE OR REPLACE FUNCTION asian_media.manage_publication_volumes()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
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
$BODY$;

CREATE TYPE asian_media.publication_language AS ENUM
    ('KOREAN', 'CHINESE', 'JAPANESE');

CREATE TYPE asian_media.publication_status AS ENUM
    ('NOT_READING', 'READING', 'COMPLETED', 'PLAN_TO_READ');

CREATE TYPE asian_media.publication_type AS ENUM
    ('MANGA', 'LIGHT_NOVEL', 'WEB_NOVEL', 'PUBLISHED_NOVEL', 'MANHWA', 'MANHUA', 'WEBTOON');

-- GAMES

CREATE TABLE IF NOT EXISTS games.characters
(
    game_id integer NOT NULL,
    has_distinctive_character boolean NOT NULL,
    has_distinctive_protagonist boolean NOT NULL,
    has_distinctive_antagonist boolean NOT NULL,
    CONSTRAINT game_characters_pkey PRIMARY KEY (game_id),
    CONSTRAINT game_characters_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
        NOT VALID
);

CREATE TABLE IF NOT EXISTS games.games
(
    id SERIAL,
    name text COLLATE pg_catalog."default" NOT NULL,
    series integer,
    status games.game_status NOT NULL,
    CONSTRAINT games_pkey PRIMARY KEY (id),
    CONSTRAINT games_games_name_uindex UNIQUE (name),
    CONSTRAINT games_series_fkey FOREIGN KEY (series)
        REFERENCES games.series (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);

CREATE TABLE IF NOT EXISTS games.play_time
(
    game_id integer NOT NULL,
    completion_count integer,
    play_time_hours numeric(10,2) NOT NULL,
    play_time_interval interval(2), -- aktualnie nieużywane
    CONSTRAINT game_play_time_pkey PRIMARY KEY (game_id),
    CONSTRAINT game_play_time_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.ratings
(
    game_id integer NOT NULL,
    story_rating integer,
    gameplay_rating integer,
    atmosphere_rating integer,
    CONSTRAINT game_ratings_pkey PRIMARY KEY (game_id),
    CONSTRAINT game_ratings_game_id_fkey FOREIGN KEY (game_id)
        REFERENCES games.games (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games.series
(
    id SERIAL,
    name text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT game_series_pkey PRIMARY KEY (id),
    CONSTRAINT game_series_name_uindex UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS games.time_played_check_time
(
    check_time timestamp without time zone NOT NULL
);

CREATE TYPE games.game_status AS ENUM
    ('NOT_PLAYING', 'WITHOUT_THE_END', 'PLAYED', 'TO_PLAY', 'PLAYING');

CREATE OR REPLACE VIEW games.time_played
AS
SELECT sum(play_time_hours) AS time_played
FROM games.play_time;

-- PUBLIC

CREATE OR REPLACE FUNCTION public.update_modified_column()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$;

-- CONFIGURATIONS

CREATE TABLE IF NOT EXISTS public.report_configurations
(
    id SERIAL,
    name text COLLATE pg_catalog."default" NOT NULL,
    report_name text COLLATE pg_catalog."default" NOT NULL,
    description text COLLATE pg_catalog."default",
    sort_order sort_configuration[],
    visible_columns text[] COLLATE pg_catalog."default",
    column_order text[] COLLATE pg_catalog."default",
    page_size integer NOT NULL DEFAULT 10,
    is_default boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT report_configurations_pkey PRIMARY KEY (id),
    CONSTRAINT report_configurations_name_type_unique UNIQUE (name, report_name)
);

CREATE OR REPLACE TRIGGER update_report_configurations_modtime
    BEFORE UPDATE
    ON public.report_configurations
    FOR EACH ROW
EXECUTE FUNCTION public.update_modified_column();

CREATE TYPE public.sort_direction AS ENUM
    ('ASCENDING', 'DESCENDING');

CREATE TYPE public.sort_configuration AS
(
    column_name text,
    sort_direction sort_direction
);

-- FILTRY
CREATE TYPE public.null_handling AS ENUM('INCLUDE', 'EXCLUDE', 'ONLY_NULL');

CREATE TYPE public.string_filter_data_type AS ENUM('EXACT', 'STARTS_WITH', 'ENDS_WITH', 'CONTAINS', 'NOT_CONTAINS');

CREATE TYPE public.number_filter_data_type AS ENUM('EQUALS', 'NOT_EQUALS', 'LESS_THAN', 'LESS_EQUALS', 'GREATER_THAN',
'GREATER_EQUALS', 'RANGE');

CREATE TYPE public.filter_data_type AS ENUM
    ('BOOLEAN', 'STRING', 'NUMBER', 'ENUM');

CREATE TABLE IF NOT EXISTS public.report_filter_configs
(
    -- Wspólne kolumny
    id SERIAL,
    report_config_id integer NOT NULL REFERENCES public.report_configurations(id) ON DELETE CASCADE,
    column_name text NOT NULL,
    null_handling null_handling NOT NULL DEFAULT 'INCLUDE',
    filter_type filter_data_type NOT NULL, -- To jest klucz, po którym dzielimy dane

    -- Kolumny specyficzne dla filtra BOOLEAN
    boolean_value boolean,

    -- Kolumny specyficzne dla filtra STRING
    string_filter_type string_filter_data_type,
    string_value text,
    case_sensitive boolean,

    -- Kolumny specyficzne dla filtra NUMBER
    number_filter_type number_filter_data_type,
    min_value numeric,
    max_value numeric,

    -- Kolumny specyficzne dla filtra ENUM
    enum_type_name text,
    enum_values text[],
    include_enum boolean,

    PRIMARY KEY(id),
    UNIQUE (report_config_id, column_name),

    -- BOOLEAN
    CONSTRAINT BOOLEAN_FILTER_CHECK CHECK (
        NOT filter_type = 'BOOLEAN' OR
        string_filter_type IS NULL AND string_value IS NULL AND case_sensitive IS NULL AND
        number_filter_type IS NULL AND min_value IS NULL AND max_value IS NULL AND
        enum_type_name IS NULL AND enum_values IS NULL AND include_enum IS NULL
    ),
    -- STRING
    CONSTRAINT STRING_FILTER_CHECK CHECK (
        NOT filter_type = 'STRING' OR
        boolean_value IS NULL AND
        string_filter_type IS NOT NULL AND case_sensitive IS NOT NULL AND
        number_filter_type IS NULL AND min_value IS NULL AND max_value IS NULL AND
        enum_type_name IS NULL AND enum_values IS NULL AND include_enum IS NULL
    ),
    -- NUMBER
    CONSTRAINT NUMBER_FILTER_CHECK CHECK (
        NOT filter_type = 'NUMBER' OR
        boolean_value IS NULL AND
        string_filter_type IS NULL AND string_value IS NULL AND case_sensitive IS NULL AND
        number_filter_type IS NOT NULL AND
        (max_value IS NULL OR min_value IS NULL OR min_value <= max_value) AND -- min_value musi być mniejsze lub równe max_value, jeśli oba istnieją
        enum_type_name IS NULL AND enum_values IS NULL AND include_enum IS NULL
    ),
    -- ENUM
    CONSTRAINT ENUM_FILTER_CHECK CHECK (
        NOT filter_type = 'ENUM' OR
        boolean_value IS NULL AND
        string_filter_type IS NULL AND string_value IS NULL AND case_sensitive IS NULL AND
        number_filter_type IS NULL AND min_value IS NULL AND max_value IS NULL AND
        enum_type_name IS NOT NULL AND enum_values IS NOT NULL AND include_enum IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_report_filter_configs_report_config_id ON public.report_filter_configs (report_config_id);