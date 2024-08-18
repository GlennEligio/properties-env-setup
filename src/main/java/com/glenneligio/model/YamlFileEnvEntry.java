package com.glenneligio.model;

import lombok.Data;

@Data
public class YamlFileEnvEntry {
    private String envName;
    private String envValue;
    private boolean isSecret;

    public YamlFileEnvEntry(String envName, String envValue, boolean isSecret) {
        this.envName = envName;
        this.envValue = envValue;
        this.isSecret = isSecret;
    }

    public String getEnvName() {
        return envName;
    }

    public String getEnvValue() {
        return envValue;
    }

    public boolean isSecret() {
        return isSecret;
    }
}
