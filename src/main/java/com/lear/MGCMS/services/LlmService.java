package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.LlmConfig;
import com.lear.MGCMS.payload.LlmChatResponse;
import com.lear.MGCMS.payload.LlmStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Autowired
    private LlmInferenceEngine inferenceEngine;

    @Autowired
    private LlmConfig llmConfig;

    @Autowired
    private ApplicationContext context;

    // Per-user conversation history: username -> list of messages
    private final ConcurrentHashMap<String, List<ChatEntry>> conversationHistory = new ConcurrentHashMap<>();

    // Session tracking: sessionId -> username
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionLastActivity = new ConcurrentHashMap<>();

    private static final int MAX_HISTORY_PER_USER = 6;
    private static final Pattern SIMPLE_GREETING_PATTERN = Pattern.compile(
            "^(hello|hi|hey|bonjour|bonsoir|salut|coucou|good morning|good afternoon|good evening)(\\s+(there|team|assistant|admin|mg cms|mg-cms))?[!?.]*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String DATABASE_SCHEMA = """
            Database: MG_CMS (SQL Server). Key tables and columns:
            - CuttingPlan: id, reference, creationDate, status, machineName
            - CuttingRequest: id, sequence, dueDate, status, partNumber, quantity, priority
            - CuttingRequestSerie: id, serie, cuttingRequestId, status, creationDate
            - CuttingRequestBox: id, serieId, boxNumber, weight, partNumber
            - CuttingRequestPartNumber: id, cuttingRequestId, partNumber, quantity
            - MachineCnc: id, machineName, status, type
            - PartNumberInfo: partNumber, description, weight, totalPerimetre, tempsDeCoupe
            - PartNumberWeight: partNumber, weight, source
            - Planning: id, machineName, date, shift, status
            - BoxWeight: id, partNumber, weight, createdAt
            - CncProduction: id, machineId, date, quantity, partNumber
            - MachineScheduleStatus: id, machineName, date, status
            - ScheduleInterval: id, machineName, startTime, endTime
            - users: id, username, fullName, role
            """;

    private static final String SYSTEM_PROMPT = """
            You are MG-CMS Assistant, a concise AI helper for the Cutting Management System at Lear Corporation.
            You help with cutting plans, quality, machines, scheduling, inventory, KPIs, and part numbers.

            IMPORTANT BEHAVIOR:
            - For greetings (hello, hi, bonjour), reply with a SHORT friendly greeting (1-2 sentences max). Do NOT list examples or SQL queries.
            - Only use [SQL_QUERY] tags when the user explicitly asks for data.
            - Be concise. Do not repeat the system instructions or list example queries.
            - Respond in the same language as the user (French or English).

            DATABASE QUERY TOOL:
            When the user asks for specific data, include ONE SQL query in this exact format:
            [SQL_QUERY]SELECT TOP 10 columns FROM Table WHERE condition[/SQL_QUERY]

            SQL RULES:
            - Only SELECT with TOP. Use exact table/column names from the schema.
            - Dates: CAST(GETDATE() AS DATE). No INSERT/UPDATE/DELETE/DROP.
            - No markdown fences inside the tags. One query per response.

            """ + DATABASE_SCHEMA + """
            """;

    @PostConstruct
    public void init() {
        // Create log directory
        try {
            Files.createDirectories(Path.of(llmConfig.getLogPath()));
        } catch (IOException e) {
            log.warn("Could not create LLM log directory: {}", llmConfig.getLogPath(), e);
        }
        // Start session cleanup timer
        startSessionCleanup();
    }

    /**
     * Chat with the LLM (blocking, full response).
     */
    public LlmChatResponse chat(String message, String sessionId, String username, String pageContext, String userRole) {
        if (!llmConfig.isEnabled()) {
            return new LlmChatResponse("AI assistant is disabled.", sessionId, "assistant", "none");
        }

        // Track session
        sessionUsers.put(sessionId, username);
        sessionLastActivity.put(sessionId, System.currentTimeMillis());

        // Build conversation with history
        List<ChatEntry> history = conversationHistory.computeIfAbsent(username, k -> new ArrayList<>());

        // Add user message to history
        history.add(new ChatEntry("user", message));

        String quickResponse = buildQuickGreetingResponse(message);
        if (quickResponse != null) {
            history.add(new ChatEntry("assistant", quickResponse));
            trimHistory(history);
            logConversation(username, message, quickResponse);
            return new LlmChatResponse(quickResponse, sessionId, "assistant", inferenceEngine.getLoadedModelName());
        }

        // Build the full prompt
        String fullPrompt = buildPrompt(history, pageContext, userRole, username);

        // Generate response
        String response = inferenceEngine.generate(fullPrompt);

        // Check for [SQL_QUERY]...[/SQL_QUERY] tags in the response
        String sqlResult = extractAndExecuteSqlQuery(response, userRole);
        if (sqlResult != null && !sqlResult.isEmpty()) {
            response = response + "\n\n**Database Result:**\n" + sqlResult;
        }

        // Add assistant response to history
        history.add(new ChatEntry("assistant", response));

        // Trim history if too long
        trimHistory(history);

        // Log the conversation
        logConversation(username, message, response);

        return new LlmChatResponse(response, sessionId, "assistant", inferenceEngine.getLoadedModelName());
    }

    /**
     * Stream chat response via a token callback.
     * After streaming completes, checks for [SQL_QUERY]...[/SQL_QUERY] tags
     * and executes the query, sending results as additional tokens.
     */
    public void chatStreaming(String message, String sessionId, String username,
                              String pageContext, String userRole,
                              java.util.function.Consumer<String> tokenCallback, Runnable onComplete) {
        if (!llmConfig.isEnabled()) {
            tokenCallback.accept("AI assistant is disabled.");
            onComplete.run();
            return;
        }

        sessionUsers.put(sessionId, username);
        sessionLastActivity.put(sessionId, System.currentTimeMillis());

        List<ChatEntry> history = conversationHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(new ChatEntry("user", message));

        String quickResponse = buildQuickGreetingResponse(message);
        if (quickResponse != null) {
            tokenCallback.accept(quickResponse);
            history.add(new ChatEntry("assistant", quickResponse));
            trimHistory(history);
            logConversation(username, message, quickResponse);
            onComplete.run();
            return;
        }

        String fullPrompt = buildPrompt(history, pageContext, userRole, username);

        StringBuilder fullResponse = new StringBuilder();

        inferenceEngine.generateStreaming(fullPrompt, token -> {
            fullResponse.append(token);
            tokenCallback.accept(token);
        }, () -> {
            String responseText = fullResponse.toString().trim();

            // Check for [SQL_QUERY]...[/SQL_QUERY] tags in the response
            String sqlResult = extractAndExecuteSqlQuery(responseText, userRole);
            if (sqlResult != null && !sqlResult.isEmpty()) {
                String resultBlock = "\n\n**Database Result:**\n" + sqlResult;
                try {
                    tokenCallback.accept(resultBlock);
                } catch (Exception ignored) {
                    // Emitter may be closed, still log the result
                }
                responseText = responseText + resultBlock;
            }

            history.add(new ChatEntry("assistant", responseText));
            trimHistory(history);
            logConversation(username, message, responseText);
            onComplete.run();
        });
    }

    /**
     * Get LLM status information.
     */
    public LlmStatusResponse getStatus() {
        LlmStatusResponse status = new LlmStatusResponse();
        status.setEnabled(llmConfig.isEnabled());
        status.setModelLoaded(inferenceEngine.isModelLoaded());
        status.setModelName(inferenceEngine.getLoadedModelName());
        status.setRamTier(llmConfig.getRamTier());

        long systemRam = ((com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean())
                .getTotalMemorySize() / (1024L * 1024L);
        status.setAvailableRamMB(systemRam);
        status.setActiveSessions(sessionUsers.size());

        if (!llmConfig.isEnabled()) {
            status.setStatus("disabled");
        } else if (inferenceEngine.isLoading()) {
            status.setStatus("loading");
        } else if (inferenceEngine.isModelLoaded()) {
            status.setStatus("ready");
        } else {
            status.setStatus("error");
            status.setErrorMessage(inferenceEngine.getErrorMessage());
        }

        return status;
    }

    /**
     * Clear conversation history for a user.
     */
    public void clearHistory(String username) {
        conversationHistory.remove(username);
        log.info("Cleared chat history for user: {}", username);
    }

    /**
     * Get conversation history for a user.
     */
    public List<LlmChatResponse> getHistory(String username, String sessionId) {
        List<ChatEntry> history = conversationHistory.getOrDefault(username, Collections.emptyList());
        return history.stream()
                .map(e -> new LlmChatResponse(e.content, sessionId, e.role, inferenceEngine.getLoadedModelName()))
                .collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    /**
     * Extract SQL query from [SQL_QUERY]...[/SQL_QUERY] tags in the LLM response
     * and execute it safely.
     */
    private String extractAndExecuteSqlQuery(String responseText, String userRole) {
        if (responseText == null) return null;

        // Use regex for more flexible matching (handles whitespace, newlines around tags)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\[SQL_QUERY\\]\\s*(.*?)\\s*\\[/SQL_QUERY\\]",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(responseText);

        if (!matcher.find()) {
            return null;
        }

        String sqlQuery = matcher.group(1).trim();
        log.info("LLM generated SQL query: {}", sqlQuery);

        return executeSafeQuery(sqlQuery);
    }

    /**
     * Execute a read-only SQL query with security validation.
     * Returns formatted markdown table or error message.
     */
    private String executeSafeQuery(String sqlQuery) {
        try {
            if (sqlQuery == null || sqlQuery.isBlank()) return null;

            // Remove any markdown code fences the LLM might have added inside the tag
            sqlQuery = sqlQuery.replaceAll("```sql\\s*", "").replaceAll("```\\s*", "").trim();

            String upper = sqlQuery.toUpperCase();

            // Security: only allow SELECT statements
            if (!upper.startsWith("SELECT")) {
                log.warn("Blocked non-SELECT query: {}", sqlQuery);
                return "_Query blocked: only SELECT statements are allowed._";
            }

            // Block dangerous keywords
            String[] blocked = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "EXEC", "TRUNCATE",
                    "CREATE", "GRANT", "REVOKE", "MERGE", "xp_", "sp_"};
            for (String keyword : blocked) {
                if (upper.contains(keyword)) {
                    log.warn("Blocked query containing '{}': {}", keyword, sqlQuery);
                    return "_Query blocked: forbidden keyword '" + keyword + "' detected._";
                }
            }

            // Block SQL comments and statement terminators
            if (sqlQuery.contains("--") || sqlQuery.contains(";") || sqlQuery.contains("/*")) {
                log.warn("Blocked query with comments/terminators: {}", sqlQuery);
                return "_Query blocked: comments and statement terminators are not allowed._";
            }

            DataSource ds = (DataSource) context.getBean("dataSource");
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            jdbc.setQueryTimeout(10); // 10 second timeout

            // Ensure results are limited
            if (!upper.contains("TOP")) {
                sqlQuery = sqlQuery.replaceFirst("(?i)SELECT", "SELECT TOP 20");
            }

            List<Map<String, Object>> results = jdbc.queryForList(sqlQuery);
            if (results.isEmpty()) {
                return "_No results found._";
            }

            // Format as markdown table
            StringBuilder table = new StringBuilder();
            Set<String> columns = results.get(0).keySet();
            table.append("| ").append(String.join(" | ", columns)).append(" |\n");
            table.append("| ").append(columns.stream().map(c -> "---").collect(Collectors.joining(" | "))).append(" |\n");

            for (Map<String, Object> row : results) {
                table.append("| ");
                for (String col : columns) {
                    Object val = row.get(col);
                    String cellValue = val != null ? val.toString() : "NULL";
                    // Escape pipe characters in values
                    cellValue = cellValue.replace("|", "\\|");
                    // Truncate long values
                    if (cellValue.length() > 50) {
                        cellValue = cellValue.substring(0, 47) + "...";
                    }
                    table.append(cellValue).append(" | ");
                }
                table.append("\n");
            }

            table.append("\n_").append(results.size()).append(" row(s) returned._");
            return table.toString();

        } catch (Exception e) {
            log.debug("Database query execution failed: {}", e.getMessage());
            return "_Query error: " + e.getMessage().replaceAll("[\\r\\n]", " ").substring(0, Math.min(e.getMessage().length(), 200)) + "_";
        }
    }

    private String buildPrompt(List<ChatEntry> history, String pageContext, String userRole, String username) {
        String modelName = inferenceEngine.getLoadedModelName().toLowerCase(Locale.ROOT);
        boolean isMistral = modelName.contains("mistral");
        boolean isQwen = modelName.contains("qwen");

        StringBuilder systemContext = new StringBuilder(SYSTEM_PROMPT);
        if (pageContext != null && !pageContext.isBlank()) {
            systemContext.append("\nThe user is currently on the '").append(sanitizeInput(pageContext)).append("' page of MG-CMS.\n");
        }
        if (userRole != null && !userRole.isBlank()) {
            systemContext.append("The user's role is: ").append(sanitizeInput(userRole)).append("\n");
        }
        systemContext.append("The user's name is: ").append(sanitizeInput(username)).append("\n");

        int maxEntries = Math.min(history.size(), 6);
        List<ChatEntry> recentHistory = history.subList(history.size() - maxEntries, history.size());

        if (isMistral) {
            return buildMistralPrompt(systemContext.toString(), recentHistory);
        } else {
            return buildChatMlPrompt(systemContext.toString(), recentHistory, isQwen);
        }
    }

    /**
     * Build prompt using Mistral [INST] / [/INST] format.
     * Note: Do NOT prepend &lt;s&gt; - the tokenizer adds BOS automatically.
     */
    private String buildMistralPrompt(String systemContext, List<ChatEntry> recentHistory) {
        StringBuilder prompt = new StringBuilder();

        boolean firstUser = true;
        for (ChatEntry entry : recentHistory) {
            if ("user".equals(entry.role)) {
                prompt.append("[INST] ");
                if (firstUser) {
                    // Include system prompt with the first user message
                    prompt.append(systemContext).append("\n\n");
                    firstUser = false;
                }
                prompt.append(entry.content).append(" [/INST]");
            } else {
                prompt.append(entry.content).append("</s>");
            }
        }

        // If history ended on assistant, add a new [INST] block (shouldn't happen normally)
        // The last entry should be the current user message, so the prompt ends after [/INST]
        return prompt.toString();
    }

    /**
     * Build prompt using ChatML <|im_start|> / <|im_end|> format (for TinyLlama, Qwen, etc.).
     */
    private String buildChatMlPrompt(String systemContext, List<ChatEntry> recentHistory, boolean disableThinking) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("<|im_start|>system\n");
        prompt.append(systemContext);
        prompt.append("<|im_end|>\n");

        for (int index = 0; index < recentHistory.size(); index++) {
            ChatEntry entry = recentHistory.get(index);
            if ("user".equals(entry.role)) {
                String content = entry.content;
                boolean isLastEntry = index == recentHistory.size() - 1;
                if (disableThinking && isLastEntry && !content.contains("/no_think") && !content.contains("/think")) {
                    content = content.stripTrailing() + "\n/no_think";
                }
                prompt.append("<|im_start|>user\n").append(content).append("<|im_end|>\n");
            } else {
                prompt.append("<|im_start|>assistant\n").append(entry.content).append("<|im_end|>\n");
            }
        }

        prompt.append("<|im_start|>assistant\n");
        return prompt.toString();
    }

    private String buildQuickGreetingResponse(String message) {
        if (!isSimpleGreeting(message)) {
            return null;
        }

        String normalized = normalizeMessage(message);
        if (normalized.startsWith("bonjour") || normalized.startsWith("bonsoir")
                || normalized.startsWith("salut") || normalized.startsWith("coucou")) {
            return "Bonjour! Comment puis-je vous aider aujourd'hui dans MG-CMS ?";
        }
        return "Hello! How can I help you with MG-CMS today?";
    }

    private boolean isSimpleGreeting(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return SIMPLE_GREETING_PATTERN.matcher(normalizeMessage(message)).matches();
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message
                .trim()
                .replaceAll("[\\s\\u00A0]+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private void trimHistory(List<ChatEntry> history) {
        while (history.size() > MAX_HISTORY_PER_USER * 2) {
            history.remove(0);
        }
    }

    private void logConversation(String username, String userMessage, String assistantResponse) {
        try {
            Path logDir = Path.of(llmConfig.getLogPath());
            Files.createDirectories(logDir);

            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path logFile = logDir.resolve("chat_" + dateStr + ".log");

            String logEntry = String.format("[%s] User: %s\nQ: %s\nA: %s\n---\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    username, userMessage, assistantResponse);

            Files.writeString(logFile, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // Cleanup old logs
            cleanupOldLogs(logDir);

        } catch (IOException e) {
            log.warn("Failed to write chat log: {}", e.getMessage());
        }
    }

    private void cleanupOldLogs(Path logDir) {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(llmConfig.getLogRetentionDays());
            Files.list(logDir)
                    .filter(p -> p.getFileName().toString().startsWith("chat_") && p.getFileName().toString().endsWith(".log"))
                    .forEach(p -> {
                        try {
                            String dateStr = p.getFileName().toString().replace("chat_", "").replace(".log", "");
                            LocalDate fileDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                            if (fileDate.isBefore(cutoff)) {
                                Files.deleteIfExists(p);
                                log.info("Deleted old chat log: {}", p.getFileName());
                            }
                        } catch (Exception ignored) {}
                    });
        } catch (Exception e) {
            log.debug("Log cleanup error: {}", e.getMessage());
        }
    }

    private void startSessionCleanup() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    long timeout = llmConfig.getSessionTimeoutMinutes() * 60000L;
                    long now = System.currentTimeMillis();

                    sessionLastActivity.forEach((sessionId, lastActivity) -> {
                        if (now - lastActivity > timeout) {
                            String username = sessionUsers.remove(sessionId);
                            sessionLastActivity.remove(sessionId);
                            if (username != null) {
                                conversationHistory.remove(username);
                                log.info("Session auto-closed for user: {}", username);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.debug("Session cleanup error: {}", e.getMessage());
                }
            }
        }, "llm-session-cleanup").start();
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>]", "").substring(0, Math.min(input.length(), 200));
    }

    // Inner class for chat entries
    private static class ChatEntry {
        final String role;
        final String content;
        final long timestamp;

        ChatEntry(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
