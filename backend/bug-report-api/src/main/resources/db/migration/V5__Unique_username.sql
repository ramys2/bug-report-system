-- Existing duplicate names must be resolved manually before applying this migration.
-- Keep the column's collation, so the constraint and existsByName use the same comparison.
ALTER TABLE user_account
    ADD CONSTRAINT uq_user_account_name UNIQUE (name);
