CREATE TABLE deltaker
(
    id                    UUID PRIMARY KEY,
    deltakerliste_id      UUID                     NOT NULL,
    personident           VARCHAR                  NOT NULL,
    fornavn               VARCHAR                  NOT NULL,
    mellomnavn            VARCHAR,
    etternavn             VARCHAR                  NOT NULL,
    telefonnummer         VARCHAR,
    epost                 VARCHAR,
    er_skjermet           BOOLEAN                  NOT NULL,
    status                VARCHAR                  NOT NULL,
    status_gyldig_fra     TIMESTAMP WITH TIME ZONE NOT NULL,
    status_opprettet_dato TIMESTAMP WITH TIME ZONE NOT NULL,
    dager_per_uke         INTEGER,
    prosent_stilling      FLOAT,
    start_dato            DATE,
    slutt_dato            DATE,
    innsokt_dato          DATE,
    bestillingstekst      VARCHAR,
    navkontor             VARCHAR,
    navveileder_id        UUID,
    navveileder_navn      VARCHAR,
    navveileder_epost     VARCHAR,
    skjult_av_ansatt_id   UUID,
    skjult_dato           TIMESTAMP WITH TIME ZONE,
    modified_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);