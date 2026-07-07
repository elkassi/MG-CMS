package com.lear.MGCMS.payload;

import java.time.LocalDateTime;

public class LlmChatResponse {
    private String message;
    private String sessionId;
    private String role; // "assistant" or "user"
    private LocalDateTime timestamp;
    private String modelName;
    private boolean streaming;

    public LlmChatResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public LlmChatResponse(String message, String sessionId, String role, String modelName) {
        this.message = message;
        this.sessionId = sessionId;
        this.role = role;
        this.timestamp = LocalDateTime.now();
        this.modelName = modelName;
        this.streaming = false;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public boolean isStreaming() { return streaming; }
    public void setStreaming(boolean streaming) { this.streaming = streaming; }
}
