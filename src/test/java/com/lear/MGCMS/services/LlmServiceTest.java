package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.LlmConfig;
import com.lear.MGCMS.payload.LlmChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmServiceTest {

    @Test
    void chatReturnsQuickGreetingWithoutInvokingTheModel() {
        LlmInferenceEngine inferenceEngine = mock(LlmInferenceEngine.class);
        when(inferenceEngine.getLoadedModelName()).thenReturn("test-model");

        LlmService service = createService(inferenceEngine);

        LlmChatResponse response = service.chat("hello", "session-1", "mouad", null, "ROLE_USER");

        assertEquals("Hello! How can I help you with MG-CMS today?", response.getMessage());
        verify(inferenceEngine, never()).generate(anyString());
        assertEquals(2, service.getHistory("mouad", "session-1").size());
    }

    @Test
    void chatStreamingReturnsQuickGreetingWithoutInvokingTheModel() {
        LlmInferenceEngine inferenceEngine = mock(LlmInferenceEngine.class);
        when(inferenceEngine.getLoadedModelName()).thenReturn("test-model");

        LlmService service = createService(inferenceEngine);
        List<String> streamedTokens = new ArrayList<>();
        boolean[] completed = {false};

        service.chatStreaming(
                "bonjour",
                "session-2",
                "mouad",
                null,
                "ROLE_USER",
                streamedTokens::add,
                () -> completed[0] = true
        );

        assertEquals(List.of("Bonjour! Comment puis-je vous aider aujourd'hui dans MG-CMS ?"), streamedTokens);
        assertTrue(completed[0]);
        verify(inferenceEngine, never()).generateStreaming(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertEquals(2, service.getHistory("mouad", "session-2").size());
    }

    private LlmService createService(LlmInferenceEngine inferenceEngine) {
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setEnabled(true);
        llmConfig.setLogPath("/tmp/mg-cms-llm-tests");
        llmConfig.setSessionTimeoutMinutes(30);

        LlmService service = new LlmService();
        ReflectionTestUtils.setField(service, "inferenceEngine", inferenceEngine);
        ReflectionTestUtils.setField(service, "llmConfig", llmConfig);
        return service;
    }
}
