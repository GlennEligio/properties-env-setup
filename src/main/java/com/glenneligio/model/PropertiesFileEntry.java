package com.glenneligio.model;

import lombok.Data;

@Data
public class PropertiesFileEntry {

    private String name;
    private String envUsed;
    private String defaultValue;
    private boolean isValid;
    private boolean isValueInjected;
    private String envValueToInject;
    private boolean isEnvValueSecret;
    private int lineNumber;
    private boolean isInjected;
    private boolean isPresentInYaml;

    public PropertiesFileEntry(String name,
                               String envUsed,
                               String defaultValue,
                               boolean isValid,
                               boolean isValueInjected,
                               int lineNumber,
                               boolean isPresentInYaml) {
        this.name = name;
        this.envUsed = envUsed;
        this.defaultValue = defaultValue;
        this.isValid = isValid;
        this.isValueInjected = isValueInjected;
        this.lineNumber = lineNumber;
        this.isPresentInYaml = isPresentInYaml;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvUsed() {
        return envUsed;
    }

    public void setEnvUsed(String envUsed) {
        this.envUsed = envUsed;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean isValueInjected() {
        return isValueInjected;
    }

    public void setValueInjected(boolean valueInjected) {
        isValueInjected = valueInjected;
    }

    public String getEnvValueToInject() {
        return envValueToInject;
    }

    public void setEnvValueToInject(String envValueToInject) {
        this.envValueToInject = envValueToInject;
    }

    public boolean isEnvValueSecret() {
        return isEnvValueSecret;
    }

    public void setEnvValueSecret(boolean envValueSecret) {
        isEnvValueSecret = envValueSecret;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isInjected() {
        return isInjected;
    }

    public void setInjected(boolean injected) {
        isInjected = injected;
    }

    public boolean isPresentInYaml() {
        return isPresentInYaml;
    }

    public void setPresentInYaml(boolean presentInYaml) {
        isPresentInYaml = presentInYaml;
    }
}
