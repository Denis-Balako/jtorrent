CREATE TABLE torrent_piece
(
    id            UUID        PRIMARY KEY  DEFAULT gen_random_uuid(),
    session_id    UUID        NOT NULL     REFERENCES torrent_session (id),
    piece_index   INT         NOT NULL,
    expected_hash BYTEA       NOT NULL,
    status        VARCHAR(20) NOT NULL     DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL     DEFAULT now(),

    CONSTRAINT uq_torrent_piece_session_index UNIQUE (session_id, piece_index)
);
