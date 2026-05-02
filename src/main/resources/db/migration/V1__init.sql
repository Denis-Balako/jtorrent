CREATE TABLE torrent_session
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(500) NOT NULL,
    info_hash  CHAR(40)     NOT NULL UNIQUE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'STOPPED',
    total_size BIGINT,
    downloaded BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
