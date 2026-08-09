-- Suppliers: the distributors that invoices come from.
--
-- First table because its shape is the one that is not in question. Items,
-- recipes and invoices wait until their domain models are settled, because a
-- migration is never edited once it has been committed -- corrections arrive as
-- a new numbered file, so guessing early is expensive.
--
-- No DEFAULT on the id: identifiers are created in the domain, not by the
-- database, so an aggregate has identity before anything is saved.

CREATE TABLE suppliers (
    id         UUID PRIMARY KEY,
    name       TEXT NOT NULL,
    category   TEXT,
    contact    TEXT,
    phone      TEXT,
    email      TEXT,
    terms      TEXT,
    address    TEXT,
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Price history per supplier is meaningless if the same distributor can be
-- entered twice under different capitalisation.
CREATE UNIQUE INDEX suppliers_name_unique ON suppliers (LOWER(name));
