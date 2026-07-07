package com.lear.MGCMS.payload;

public class LlmChatRequest {
    private String message;
    private String sessionId;
    private String pageContext; // current page the user is on

    public LlmChatRequest() {}

    public LlmChatRequest(String message, String sessionId, String pageContext) {
        this.message = message;
        this.sessionId = sessionId;
        this.pageContext = pageContext;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPageContext() { return pageContext; }
    public void setPageContext(String pageContext) { this.pageContext = pageContext; }
}
