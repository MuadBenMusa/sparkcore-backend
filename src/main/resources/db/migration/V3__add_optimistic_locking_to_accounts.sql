-- Fügt die "version" Spalte für Optimistic Locking (@Version in JPA) hinzu.
-- Default ist 0 für bereits existierende Konten.
ALTER TABLE accounts ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
