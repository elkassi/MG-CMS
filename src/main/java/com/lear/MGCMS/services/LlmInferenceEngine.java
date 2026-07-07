package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.LlmConfig;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.args.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
public class LlmInferenceEngine {

    private static final Logger log = LoggerFactory.getLogger(LlmInferenceEngine.class);
    private static final long MEBIBYTE = 1024L * 1024L;
    private static final String NATIVE_LIBRARY_BASE_NAME = "jllama";
    private static final Pattern THINK_BLOCK_PATTERN = Pattern.compile("(?s)^\\s*<think>.*?</think>\\s*");

    @Autowired
    private LlmConfig llmConfig;

    private LlamaModel model;
    private volatile boolean modelLoaded = false;
    private volatile boolean loading = false;
    private String loadedModelName = "";
    private String errorMessage = "";

    @PostConstruct
    public void init() {
        if (llmConfig.isEnabled()) {
            loadModelAsync();
        } else {
            log.info("LLM is disabled via configuration (mgcms.llm.enabled=false)");
        }
    }

    @PreDestroy
    public void shutdown() {
        unloadModel();
    }

    private void unloadModel() {
        if (model != null) {
            try {
                log.info("Closing LLM model...");
                model.close();
                log.info("LLM model closed");
            } catch (Exception e) {
                log.warn("Error closing LLM model: {}", e.getMessage());
            }
            model = null;
        }
    }

    private void loadModelAsync() {
        Thread loaderThread = new Thread(() -> {
            try {
                loading = true;
                modelLoaded = false;
                loadedModelName = "";
                errorMessage = "";

                File modelFile = ensurePreferredModelFile();
                String modelFilePath = modelFile.getAbsolutePath();

                log.info("Loading LLM model: {}", modelFilePath);
                log.info("Model file size: {} MB", modelFile.length() / MEBIBYTE);

                prepareNativeLibrary();

                LlamaModel.setLogger(LogFormat.TEXT, (level, message) -> {
                    log.debug("[llama.cpp] {}: {}", level, message);
                });

                ModelParameters modelParams = new ModelParameters()
                        .setModel(modelFilePath)
                        .setGpuLayers(llmConfig.getGpuLayers())
                        .setThreads(llmConfig.getThreads())
                        .setCtxSize(llmConfig.getContextSize());

                model = new LlamaModel(modelParams);
                modelLoaded = true;
                loadedModelName = modelFile.getName();
                log.info("LLM model loaded successfully: {} (context: {}, threads: {})",
                        loadedModelName, llmConfig.getContextSize(), llmConfig.getThreads());
            } catch (Exception e) {
                errorMessage = "Failed to load LLM model: " + e.getMessage();
                log.error(errorMessage, e);
                modelLoaded = false;
            } finally {
                loading = false;
            }
        }, "llm-model-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private File ensurePreferredModelFile() throws Exception {
        File preferredModelFile = new File(llmConfig.getPreferredModelPath());
        if (preferredModelFile.exists()) {
            return preferredModelFile;
        }

        if (!llmConfig.isDownloadMissingModels()) {
            throw new IllegalStateException(buildMissingModelMessage(preferredModelFile));
        }

        String modelUrl = llmConfig.getResolvedModelUrl();
        if (modelUrl == null || modelUrl.isBlank()) {
            throw new IllegalStateException(buildMissingModelMessage(preferredModelFile)
                    + ". No download URL is configured for " + llmConfig.getPreferredModelName());
        }

        downloadModel(modelUrl, preferredModelFile.toPath());
        return preferredModelFile;
    }

    private String buildMissingModelMessage(File preferredModelFile) {
        return "Preferred model file not found: " + preferredModelFile.getAbsolutePath()
                + ". Expected model directory: " + llmConfig.getModelPath();
    }

    private void prepareNativeLibrary() throws Exception {
        String configuredNativeLibraryPath = System.getProperty("de.kherud.llama.lib.path");
        if (configuredNativeLibraryPath != null && !configuredNativeLibraryPath.isBlank()) {
            return;
        }

        String resourcePath = resolveNativeLibraryResourcePath();
        if (resourcePath == null) {
            log.warn("Could not determine bundled jllama resource path for os.name={} and os.arch={}",
                    System.getProperty("os.name"), System.getProperty("os.arch"));
            return;
        }

        Path nativeDirectory = Path.of(llmConfig.getModelPath(), "native");
        Files.createDirectories(nativeDirectory);

        Path nativeLibraryPath = nativeDirectory.resolve(System.mapLibraryName(NATIVE_LIBRARY_BASE_NAME));
        if (!Files.exists(nativeLibraryPath) || Files.size(nativeLibraryPath) == 0L) {
            try (InputStream inputStream = LlmInferenceEngine.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Bundled native library not found at " + resourcePath);
                }
                Files.copy(inputStream, nativeLibraryPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        System.setProperty("de.kherud.llama.lib.path", nativeDirectory.toString());
        log.info("Using jllama native library from {}", nativeLibraryPath);
    }

    private String resolveNativeLibraryResourcePath() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String platformDirectory;
        if (osName.contains("win")) {
            platformDirectory = "Windows";
        } else if (osName.contains("mac")) {
            platformDirectory = "Mac";
        } else if (osName.contains("linux")) {
            platformDirectory = "Linux";
        } else {
            return null;
        }

        String architectureDirectory = switch (osArch) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "x86", "i386", "i686" -> "x86";
            case "aarch64", "arm64" -> "aarch64";
            default -> osArch;
        };

        return "/de/kherud/llama/" + platformDirectory + "/" + architectureDirectory + "/"
                + System.mapLibraryName(NATIVE_LIBRARY_BASE_NAME);
    }

    private void downloadModel(String modelUrl, Path targetPath) throws Exception {
        Files.createDirectories(targetPath.getParent());
        Path partialPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".part");
        Files.deleteIfExists(partialPath);

        log.info("Downloading missing model from {}", modelUrl);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(modelUrl))
                .timeout(Duration.ofHours(3))
                .header("User-Agent", "MG-CMS/1.0")
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Model download failed with HTTP status " + statusCode + " for " + modelUrl);
        }

        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        long nextProgressLogThreshold = 64L * MEBIBYTE;
        long downloadedBytes = 0L;

        try (InputStream inputStream = response.body();
             OutputStream outputStream = Files.newOutputStream(partialPath,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                if (downloadedBytes >= nextProgressLogThreshold) {
                    log.info("Downloaded {} MB{}",
                            downloadedBytes / MEBIBYTE,
                            contentLength > 0 ? " of " + (contentLength / MEBIBYTE) + " MB" : "");
                    nextProgressLogThreshold += 64L * MEBIBYTE;
                }
            }
        } catch (Exception e) {
            Files.deleteIfExists(partialPath);
            throw e;
        }

        Files.move(partialPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Model downloaded successfully to {} ({} MB)", targetPath, downloadedBytes / MEBIBYTE);
    }

    public String generate(String prompt) {
        if (!modelLoaded || model == null) {
            return "[LLM not available] The AI assistant is currently loading or not configured. Please try again in a moment.";
        }

        try {
            InferenceParameters inferParams = new InferenceParameters(prompt)
                    .setTemperature((float) llmConfig.getTemperature())
                    .setNPredict(llmConfig.getMaxTokens())
                    .setRepeatPenalty(1.1f)
                    .setStopStrings(getStopStrings());

            String response = model.complete(inferParams);
            return sanitizeAssistantResponse(response);
        } catch (Exception e) {
            log.error("LLM inference error", e);
            return "[Error] Failed to generate response: " + e.getMessage();
        }
    }

    public void generateStreaming(String prompt, Consumer<String> tokenCallback, Runnable onComplete) {
        if (!modelLoaded || model == null) {
            tokenCallback.accept("[LLM not available] The AI assistant is currently loading or not configured.");
            onComplete.run();
            return;
        }

        new Thread(() -> {
            try {
                InferenceParameters inferParams = new InferenceParameters(prompt)
                        .setTemperature((float) llmConfig.getTemperature())
                        .setNPredict(llmConfig.getMaxTokens())
                        .setRepeatPenalty(1.1f)
                        .setStopStrings(getStopStrings());

                for (LlamaOutput output : model.generate(inferParams)) {
                    String token = output.toString();
                    if (token != null && !token.isEmpty()) {
                        try {
                            tokenCallback.accept(token);
                        } catch (Exception e) {
                            log.warn("Token delivery failed, stopping generation: {}", e.getMessage());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("LLM streaming error", e);
                try {
                    tokenCallback.accept("[Error] " + e.getMessage());
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    onComplete.run();
                } catch (Exception e) {
                    log.debug("onComplete handler error: {}", e.getMessage());
                }
            }
        }, "llm-stream-" + System.currentTimeMillis()).start();
    }

    private String sanitizeAssistantResponse(String response) {
        if (response == null) {
            return "";
        }

        String sanitizedResponse = THINK_BLOCK_PATTERN.matcher(response).replaceFirst("");
        sanitizedResponse = sanitizedResponse.replace("<|im_end|>", "")
                .replace("</s>", "")
                .trim();
        return sanitizedResponse;
    }

    private String[] getStopStrings() {
        if (loadedModelName.toLowerCase(Locale.ROOT).contains("mistral")) {
            return new String[]{"</s>", "[INST]", "<s>", "User:", "\nUser:"};
        }
        return new String[]{"</s>", "<|im_end|>", "<|endoftext|>", "<|end|>", "User:", "<|im_start|>user"};
    }

    public boolean isModelLoaded() { return modelLoaded; }
    public boolean isLoading() { return loading; }
    public String getLoadedModelName() { return loadedModelName; }
    public String getErrorMessage() { return errorMessage; }

    public void reloadModel() {
        unloadModel();
        modelLoaded = false;
        loadedModelName = "";
        errorMessage = "";
        loadModelAsync();
    }
}
