package com.lear.MGCMS.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mgcms.llm")
public class LlmConfig {

    private boolean enabled = true;
    private String modelPath = "C:\\CMS\\models";
    private String modelName = "";
    private String modelUrl = "";
    private String ramTier = "auto";
    private int contextSize = 2048;
    private int maxTokens = 512;
    private double temperature = 0.4;
    private int threads = 4;
    private int gpuLayers = 0;
    private boolean autoSelectModel = true;
    private boolean downloadMissingModels = true;
    private String logPath = "C:\\CMS\\modelsLog";
    private int logRetentionDays = 7;
    private int maxConcurrentSessions = 5;
    private int sessionTimeoutMinutes = 30;

    // Model filenames per tier
    private String model1gb = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";
    private String model2gb = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";
    private String model8gb = "Mistral-7B-Instruct-v0.3.Q4_K_M.gguf";
    private String model64gb = "Mistral-7B-Instruct-v0.3.Q4_K_M.gguf";

    // Optional model download URLs per tier
    private String model1gbUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf?download=true";
    private String model2gbUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf?download=true";
    private String model8gbUrl = "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf?download=true";
    private String model64gbUrl = "https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf?download=true";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }

    public String getRamTier() { return ramTier; }
    public void setRamTier(String ramTier) { this.ramTier = ramTier; }

    public int getContextSize() { return contextSize; }
    public void setContextSize(int contextSize) { this.contextSize = contextSize; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    public int getGpuLayers() { return gpuLayers; }
    public void setGpuLayers(int gpuLayers) { this.gpuLayers = gpuLayers; }

    public boolean isAutoSelectModel() { return autoSelectModel; }
    public void setAutoSelectModel(boolean autoSelectModel) { this.autoSelectModel = autoSelectModel; }

    public boolean isDownloadMissingModels() { return downloadMissingModels; }
    public void setDownloadMissingModels(boolean downloadMissingModels) { this.downloadMissingModels = downloadMissingModels; }

    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public int getLogRetentionDays() { return logRetentionDays; }
    public void setLogRetentionDays(int logRetentionDays) { this.logRetentionDays = logRetentionDays; }

    public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(int maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }

    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

    public String getModel1gb() { return model1gb; }
    public void setModel1gb(String model1gb) { this.model1gb = model1gb; }

    public String getModel2gb() { return model2gb; }
    public void setModel2gb(String model2gb) { this.model2gb = model2gb; }

    public String getModel8gb() { return model8gb; }
    public void setModel8gb(String model8gb) { this.model8gb = model8gb; }

    public String getModel64gb() { return model64gb; }
    public void setModel64gb(String model64gb) { this.model64gb = model64gb; }

    public String getModel1gbUrl() { return model1gbUrl; }
    public void setModel1gbUrl(String model1gbUrl) { this.model1gbUrl = model1gbUrl; }

    public String getModel2gbUrl() { return model2gbUrl; }
    public void setModel2gbUrl(String model2gbUrl) { this.model2gbUrl = model2gbUrl; }

    public String getModel8gbUrl() { return model8gbUrl; }
    public void setModel8gbUrl(String model8gbUrl) { this.model8gbUrl = model8gbUrl; }

    public String getModel64gbUrl() { return model64gbUrl; }
    public void setModel64gbUrl(String model64gbUrl) { this.model64gbUrl = model64gbUrl; }

    public String getPreferredModelName() {
        String explicitModelName = normalize(modelName);
        if (!explicitModelName.isEmpty()) {
            return explicitModelName;
        }
        return resolveModelByTier();
    }

    public String getPreferredModelPath() {
        return buildModelPath(getPreferredModelName());
    }

    /**
     * Resolves the full model file path based on configuration.
     * If modelName is set explicitly, uses that. Otherwise auto-selects based on RAM tier,
     * falling back to any model file that actually exists on disk.
     */
    public String getResolvedModelPath() {
        String preferredModelPath = getPreferredModelPath();
        if (new java.io.File(preferredModelPath).exists()) {
            return preferredModelPath;
        }

        String existingModelName = findExistingModelName();
        if (existingModelName != null) {
            return buildModelPath(existingModelName);
        }

        return preferredModelPath;
    }

    public String getResolvedModelUrl() {
        return getModelUrlFor(getPreferredModelName());
    }

    public String getModelUrlFor(String requestedModelName) {
        String explicitModelUrl = normalize(modelUrl);
        if (!explicitModelUrl.isEmpty()) {
            return explicitModelUrl;
        }

        String normalizedModelName = normalize(requestedModelName);
        if (normalizedModelName.isEmpty()) {
            return "";
        }

        if (matchesConfiguredModel(normalizedModelName, model1gb)) {
            return normalize(model1gbUrl);
        }
        if (matchesConfiguredModel(normalizedModelName, model2gb)) {
            return normalize(model2gbUrl);
        }
        if (matchesConfiguredModel(normalizedModelName, model8gb)) {
            return normalize(model8gbUrl);
        }
        if (matchesConfiguredModel(normalizedModelName, model64gb)) {
            return normalize(model64gbUrl);
        }

        return "";
    }

    public String findExistingModelName() {
        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
        addCandidate(candidates, getPreferredModelName());
        addCandidate(candidates, model1gb);
        addCandidate(candidates, model2gb);
        addCandidate(candidates, model8gb);
        addCandidate(candidates, model64gb);

        for (String candidate : candidates) {
            if (new java.io.File(buildModelPath(candidate)).exists()) {
                return candidate;
            }
        }

        java.io.File dir = new java.io.File(modelPath);
        if (!dir.isDirectory()) {
            return null;
        }

        java.io.File[] ggufFiles = dir.listFiles((ignoredDir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".gguf"));
        if (ggufFiles == null || ggufFiles.length == 0) {
            return null;
        }

        java.util.Arrays.sort(ggufFiles, java.util.Comparator.comparingLong(java.io.File::length));
        return ggufFiles[0].getName();
    }

    private String resolveModelByTier() {
        String preferred;
        if ("auto".equalsIgnoreCase(ramTier)) {
            long systemRam = ((com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize()
                    / (1024L * 1024L * 1024L);
            if (systemRam >= 48) preferred = model64gb;
            else if (systemRam >= 6) preferred = model8gb;
            else if (systemRam >= 2) preferred = model2gb;
            else preferred = model1gb;
        } else {
            preferred = switch (ramTier.toLowerCase()) {
                case "64gb" -> model64gb;
                case "8gb" -> model8gb;
                case "2gb" -> model2gb;
                case "1gb" -> model1gb;
                default -> model2gb;
            };
        }

        return preferred;
    }

    private String buildModelPath(String fileName) {
        String basePath = modelPath.endsWith("\\") || modelPath.endsWith("/") ? modelPath : modelPath + "\\";
        return basePath + normalize(fileName);
    }

    private void addCandidate(java.util.Set<String> candidates, String candidate) {
        String normalizedCandidate = normalize(candidate);
        if (!normalizedCandidate.isEmpty()) {
            candidates.add(normalizedCandidate);
        }
    }

    private boolean matchesConfiguredModel(String requestedModelName, String configuredModelName) {
        String normalizedConfiguredModelName = normalize(configuredModelName);
        return !normalizedConfiguredModelName.isEmpty()
                && requestedModelName.equalsIgnoreCase(normalizedConfiguredModelName);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
