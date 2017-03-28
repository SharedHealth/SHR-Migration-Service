package org.sharedhealth.migrationService.config;

import java.util.Map;

public class ShrProperties {
    private static ShrProperties shrProperties;
    private String shrUrl;

    public ShrProperties() {
        Map<String, String> env = System.getenv();
        this.shrUrl = env.get("SHR_SERVER_BASE_URL");
    }

    public static ShrProperties getInstance() {
        if (shrProperties != null) return shrProperties;
        shrProperties = new ShrProperties();
        return shrProperties;
    }

    public String getShrServerBaseUrl() {
        return shrUrl;
    }
}
