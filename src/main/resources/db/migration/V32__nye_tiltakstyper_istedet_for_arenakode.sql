-- Migration V32: Rename columns for tiltak and map old tiltak_type values to new tiltakskode strings
-- - tiltak_navn -> tiltaksnavn
-- - tiltak_type -> tiltakskode (mapped values)

-- Run-safe: only perform changes if the old column exists (and the new doesn't) so this migration is idempotent.

DO $$
BEGIN
    -- Rename tiltak_navn -> tiltaksnavn if needed
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='deltakerliste' AND column_name='tiltak_navn'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='deltakerliste' AND column_name='tiltaksnavn'
    ) THEN
        ALTER TABLE deltakerliste RENAME COLUMN tiltak_navn TO tiltaksnavn;
    END IF;

    -- Map tiltak_type -> tiltakskode (create new column, populate from old column values, set NOT NULL, then drop old column)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='deltakerliste' AND column_name='tiltak_type'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='deltakerliste' AND column_name='tiltakskode'
    ) THEN
        -- add new column (nullable while we populate)
        ALTER TABLE deltakerliste ADD COLUMN tiltakskode varchar;

        -- populate new column with mapped values from gamle tiltak_type
        UPDATE deltakerliste SET tiltakskode = CASE tiltak_type
            WHEN 'ARBFORB' THEN 'ARBEIDSFORBEREDENDE_TRENING'
            WHEN 'ARBRRHDAG' THEN 'ARBEIDSRETTET_REHABILITERING'
            WHEN 'AVKLARAG' THEN 'AVKLARING'
            WHEN 'DIGIOPPARB' THEN 'DIGITALT_OPPFOLGINGSTILTAK'
            WHEN 'GRUPPEAMO' THEN 'GRUPPE_ARBEIDSMARKEDSOPPLAERING'
            WHEN 'GRUFAGYRKE' THEN 'GRUPPE_FAG_OG_YRKESOPPLAERING'
            WHEN 'INDOPPFAG' THEN 'OPPFOLGING'
            WHEN 'VASV' THEN 'VARIG_TILRETTELAGT_ARBEID_SKJERMET'
            WHEN 'JOBBK' THEN 'JOBBKLUBB'
            ELSE 'UKJENT'
        END;

        -- ensure no nulls remain; if there are nulls, set them to 'UKJENT'
        UPDATE deltakerliste SET tiltakskode = 'UKJENT' WHERE tiltakskode IS NULL;

        -- make column NOT NULL
        ALTER TABLE deltakerliste ALTER COLUMN tiltakskode SET NOT NULL;

        -- drop old column
        ALTER TABLE deltakerliste DROP COLUMN tiltak_type;
    END IF;
END$$;

-- Note: PostgreSQL will update indexes/constraints that reference renamed columns. This migration intentionally creates a new column for mapped values and drops the old one to allow mapping logic.
