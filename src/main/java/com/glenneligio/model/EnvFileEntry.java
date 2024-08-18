package com.glenneligio.model;

import lombok.Data;

@Data
public class EnvFileEntry {

    private String name;
    private String defaultValue;
    private String envValueToInject;
    private boolean isValid;
    private boolean isInjected;
    private int lineNumber;
    private boolean isEnvValueSecret;
    private boolean isFromYamlEnv;
    private boolean isPresentInYaml;

    public EnvFileEntry(String name, String defaultValue, String envValueToInject, boolean isValid, boolean isInjected, int lineNumber, boolean isEnvValueSecret, boolean isFromYamlEnv, boolean isPresentInYaml) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.envValueToInject = envValueToInject;
        this.isValid = isValid;
        this.isInjected = isInjected;
        this.lineNumber = lineNumber;
        this.isEnvValueSecret = isEnvValueSecret;
        this.isFromYamlEnv = isFromYamlEnv;
        this.isPresentInYaml = isPresentInYaml;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getEnvValueToInject() {
        return envValueToInject;
    }

    public void setEnvValueToInject(String envValueToInject) {
        this.envValueToInject = envValueToInject;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean isInjected() {
        return isInjected;
    }

    public void setInjected(boolean injected) {
        isInjected = injected;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isEnvValueSecret() {
        return isEnvValueSecret;
    }

    public void setEnvValueSecret(boolean envValueSecret) {
        isEnvValueSecret = envValueSecret;
    }

    public boolean isFromYamlEnv() {
        return isFromYamlEnv;
    }

    public void setFromYamlEnv(boolean fromYamlEnv) {
        isFromYamlEnv = fromYamlEnv;
    }

    public boolean isPresentInYaml() {
        return isPresentInYaml;
    }

    public void setPresentInYaml(boolean presentInYaml) {
        isPresentInYaml = presentInYaml;
    }
}
