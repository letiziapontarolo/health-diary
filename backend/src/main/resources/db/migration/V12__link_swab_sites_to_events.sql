-- One swab site per swab event; method-specific results live in exam_results.
CREATE TABLE medical_event_swabs (
 event_id BIGINT PRIMARY KEY REFERENCES medical_events(id) ON DELETE CASCADE,
 swab_site_id BIGINT NOT NULL REFERENCES swab_sites(id) ON DELETE RESTRICT,
 other_site_text VARCHAR(200),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_swab_other_site CHECK(other_site_text IS NULL OR LENGTH(BTRIM(other_site_text))>0)
);
CREATE INDEX idx_medical_event_swabs_site ON medical_event_swabs(swab_site_id);
-- Backend must ensure event service is 'Analisi su tampone'; 'Altro' needs other_site_text.
