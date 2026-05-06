CREATE TYPE finances.account_type AS ENUM (
    'ASSET', -- Aktywa: konta bankowe, gotówka, akcje
    'LIABILITY', -- Pasywa: kredyty, pożyczki
    'INCOME', -- Przychody: wypłata, bonusy
    'EXPENSE', -- Wydatki: jedzenie, rachunki
    'EQUITY' -- Kapitał własny: bilans otwarcia
    );

CREATE TABLE finances.accounts
(
    id        INT4 PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name      TEXT                  NOT NULL,
    type      finances.account_type NOT NULL,
    currency  CHAR(3)               NOT NULL DEFAULT 'PLN',
    parent_id INTEGER REFERENCES finances.accounts (id),
    meta      JSONB
);

CREATE TABLE finances.transactions
(
    id               INT8 PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    description      TEXT        NOT NULL
);

CREATE TABLE finances.splits
(
    id             INT8 PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    transaction_id BIGINT         NOT NULL REFERENCES finances.transactions (id) ON DELETE CASCADE,
    account_id     INTEGER        NOT NULL REFERENCES finances.accounts (id) ON DELETE RESTRICT,
    -- Kwota. Dodatnia dla DEBET (wpływ na konto), ujemna dla KREDYT (wypływ z konta)
    amount         NUMERIC(19, 2) NOT NULL,
    CONSTRAINT amount_not_zero CHECK (amount <> 0)
);

-- Indeksy są kluczowe dla wydajności
CREATE INDEX ON finances.splits (transaction_id);
CREATE INDEX ON finances.splits (account_id);
