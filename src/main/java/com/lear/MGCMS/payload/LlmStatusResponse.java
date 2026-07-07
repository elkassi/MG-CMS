package com.lear.MGCMS.payload;

public class LlmStatusResponse {
    private boolean enabled;
    private boolean modelLoaded;
    private String modelName;
    private String ramTier;
    private long availableRamMB;
    private int activeSessions;
    private String status; // "ready", "loading", "disabled", "error"
    private String errorMessage;

    public LlmStatusResponse() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isModelLoaded() { return modelLoaded; }
    public void setModelLoaded(boolean modelLoaded) { this.modelLoaded = modelLoaded; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getRamTier() { return ramTier; }
    public void setRamTier(String ramTier) { this.ramTier = ramTier; }

    public long getAvailableRamMB() { return availableRamMB; }
    public void setAvailableRamMB(long availableRamMB) { this.availableRamMB = availableRamMB; }

    public int getActiveSessions() { return activeSessions; }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
