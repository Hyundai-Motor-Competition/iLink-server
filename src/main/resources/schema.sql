CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_key_principal_key
    ON idempotency_key (principal_type, principal_id, idempotency_key) NULLS NOT DISTINCT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicle_vin_linked
    ON vehicle (vin) WHERE unlinked_at IS NULL;
