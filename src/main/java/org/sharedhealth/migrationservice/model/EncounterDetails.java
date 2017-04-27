package org.sharedhealth.migrationservice.model;

import java.util.UUID;

public class EncounterDetails {
    private String encounterId;
    private UUID receivedAt;

    public EncounterDetails(String encounterId, UUID receivedAt) {
        this.encounterId = encounterId;
        this.receivedAt = receivedAt;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public UUID getReceivedAt() {
        return receivedAt;
    }
}
