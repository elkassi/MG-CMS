package com.lear.MGCMS.services.dispatcher;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes dispatch-engine state, samples and accepted moves to the
 * {@code /topic/dispatcher/engine} WebSocket topic so the Process page
 * can render a live progress panel.
 */
@Service
public class DispatchEngineWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private volatile long lastSamplePublishMs = 0;

    /** Publish the current engine lifecycle state. */
    public void publishState(EngineState state, EngineMode mode, Long runId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("state", state != null ? state.name() : null);
        body.put("mode", mode != null ? mode.name() : null);
        body.put("runId", runId);
        messagingTemplate.convertAndSend("/topic/dispatcher/engine", body);
    }

    /**
     * Publish one objective sample. Throttled to one message every 2 seconds
     * so the browser isn't flooded during fast loops.
     */
    public void publishSample(Long runId, int iteration,
                              double spread, double maxLoad, double minLoad,
                              double stdDev, double median) {
        long now = System.currentTimeMillis();
        if (now - lastSamplePublishMs < 2000) {
            return;
        }
        lastSamplePublishMs = now;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SAMPLE");
        body.put("runId", runId);
        body.put("iteration", iteration);
        body.put("spread", round2(spread));
        body.put("maxLoad", round2(maxLoad));
        body.put("minLoad", round2(minLoad));
        body.put("stdDev", round2(stdDev));
        body.put("median", round2(median));
        messagingTemplate.convertAndSend("/topic/dispatcher/engine", body);
    }

    private volatile long lastSuggestionPublishMs = 0;

    /** Publish a single accepted move so the UI can flash the row. Throttled. */
    public void publishSuggestion(Long runId, String sequence, String fromZone, String toZone) {
        long now = System.currentTimeMillis();
        if (now - lastSuggestionPublishMs < 100) {
            return;
        }
        lastSuggestionPublishMs = now;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SUGGESTION");
        body.put("runId", runId);
        body.put("sequence", sequence);
        body.put("fromZone", fromZone);
        body.put("toZone", toZone);
        messagingTemplate.convertAndSend("/topic/dispatcher/engine", body);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
