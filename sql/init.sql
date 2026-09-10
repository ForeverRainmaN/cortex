CREATE TABLE IF NOT EXISTS users (
    user_id         UUID PRIMARY KEY,
    email           TEXT NOT NULL UNIQUE,
    hashed_password TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
);

CREATE TABLE IF NOT EXISTS events (
    id              UUID PRIMARY KEY,
    kind            TEXT NULLABLE
)
