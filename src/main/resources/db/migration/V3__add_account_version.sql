-- Optimistic Locking: Füge die Version-Spalte zur accounts-Tabelle hinzu.
-- JPA/@Version nutzt diese Spalte, um gleichzeitige Überweisungen (Double-Spend) zu verhindern.
-- Startwert 0 für alle bestehenden Einträge.
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
