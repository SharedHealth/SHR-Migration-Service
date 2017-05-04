package org.sharedhealth.migrationservice.model;

import java.util.UUID;

public class EncounterDetails {
    private String encounterId;
    private UUID receivedAt;
    private int oldContentVersion;

    public EncounterDetails(String encounterId, UUID receivedAt, int oldContentVersion) {
        this.encounterId = encounterId;
        this.receivedAt = receivedAt;
        this.oldContentVersion = oldContentVersion;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public UUID getReceivedAt() {
        return receivedAt;
    }

    public int getOldContentVersion() {
        return oldContentVersion;
    }

}
