package com.lear.MGCMS.controller;

import com.lear.MGCMS.payload.LlmChatRequest;
import com.lear.MGCMS.payload.LlmChatResponse;
import com.lear.MGCMS.payload.LlmStatusResponse;
import com.lear.MGCMS.services.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/llm")
@CrossOrigin
public class LlmChatController {

    @Autowired
    private LlmService llmService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * GET /api/llm/status - Check LLM availability and status
     */
    @GetMapping("/status")
    public ResponseEntity<LlmStatusResponse> getStatus() {
        return ResponseEntity.ok(llmService.getStatus());
    }

    /**
     * POST /api/llm/chat - Send a message and get a full response (blocking)
     */
    @PostMapping("/chat")
    public ResponseEntity<LlmChatResponse> chat(@RequestBody LlmChatRequest request,
                                                  Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        String role = authentication != null ? authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")) : "ROLE_USER";

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        LlmChatResponse response = llmService.chat(
                request.getMessage(),
                sessionId,
                username,
                request.getPageContext(),
                role
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/llm/chat/stream - Stream response via Server-Sent Events
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody LlmChatRequest request,
                                  Authentication authentication) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minute timeout for slow models

        // Track whether the emitter is still active
        final AtomicBoolean emitterActive = new AtomicBoolean(true);
        emitter.onCompletion(() -> emitterActive.set(false));
        emitter.onTimeout(() -> emitterActive.set(false));
        emitter.onError(e -> emitterActive.set(false));

        String username = authentication != null ? authentication.getName() : "anonymous";
        String role = authentication != null ? authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")) : "ROLE_USER";

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        final String finalSessionId = sessionId;

        llmService.chatStreaming(
                request.getMessage(),
                finalSessionId,
                username,
                request.getPageContext(),
                role,
                token -> {
                    if (!emitterActive.get()) return;
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token));
                    } catch (Exception e) {
                        emitterActive.set(false);
                    }
                },
                () -> {
                    if (!emitterActive.get()) return;
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitterActive.set(false);
                    }
                }
        );

        return emitter;
    }

    /**
     * POST /api/llm/chat/ws - Send message and receive streaming response via WebSocket
     * The response tokens are sent to /api/topic/llm-chat/{sessionId}
     */
    @PostMapping("/chat/ws")
    public ResponseEntity<?> chatWebSocket(@RequestBody LlmChatRequest request,
                                            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        String role = authentication != null ? authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")) : "ROLE_USER";

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        final String finalSessionId = sessionId;

        llmService.chatStreaming(
                request.getMessage(),
                finalSessionId,
                username,
                request.getPageContext(),
                role,
                token -> {
                    LlmChatResponse tokenResponse = new LlmChatResponse();
                    tokenResponse.setMessage(token);
                    tokenResponse.setSessionId(finalSessionId);
                    tokenResponse.setRole("assistant");
                    tokenResponse.setStreaming(true);
                    messagingTemplate.convertAndSend(
                            "/api/topic/llm-chat/" + finalSessionId,
                            tokenResponse
                    );
                },
                () -> {
                    LlmChatResponse doneResponse = new LlmChatResponse();
                    doneResponse.setMessage("[DONE]");
                    doneResponse.setSessionId(finalSessionId);
                    doneResponse.setRole("system");
                    doneResponse.setStreaming(false);
                    messagingTemplate.convertAndSend(
                            "/api/topic/llm-chat/" + finalSessionId,
                            doneResponse
                    );
                }
        );

        return ResponseEntity.ok(java.util.Map.of("sessionId", finalSessionId, "status", "streaming"));
    }

    /**
     * GET /api/llm/history - Get conversation history for the current user
     */
    @GetMapping("/history")
    public ResponseEntity<List<LlmChatResponse>> getHistory(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(llmService.getHistory(username, sessionId));
    }

    /**
     * DELETE /api/llm/history - Clear conversation history for the current user
     */
    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        llmService.clearHistory(username);
        return ResponseEntity.ok(java.util.Map.of("message", "Chat history cleared"));
    }
}
