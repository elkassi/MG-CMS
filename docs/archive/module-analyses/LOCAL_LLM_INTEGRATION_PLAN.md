# 🤖 Local LLM Integration Plan for MG-CMS

## 📋 Table of Contents

1. [Feasibility Assessment](#feasibility-assessment)
2. [Architecture Overview](#architecture-overview)
3. [RAM Tier Strategy (2GB / 8GB / 64GB)](#ram-tier-strategy)
4. [Java Implementation Approach](#java-implementation-approach)
5. [Frontend Chat Integration](#frontend-chat-integration)
6. [LLM Recommendations for MG-CMS Features](#llm-recommendations-for-mg-cms-features)
7. [Implementation Phases](#implementation-phases)
8. [Questions](#questions)

---

## 1. Feasibility Assessment

### ✅ **YES, it is possible** — with the right approach

Running a local LLM entirely from Java, without internet access, and across different RAM tiers (2GB, 8GB, 64GB) is feasible using **quantized open-source models** and **Java-native inference libraries**.

### Key Technologies

| Technology | Purpose | Why |
|---|---|---|
| **llama.cpp** (via JNI/JNA binding) | LLM inference engine | Highly optimized C/C++ engine for running quantized GGUF models on CPU (no GPU required) |
| **jllama / llama-java** | Java binding for llama.cpp | Provides a native Java API to load and run GGUF models directly from Spring Boot |
| **GGUF quantized models** | The LLM model files | Open-source models (Llama, Mistral, Phi, TinyLlama, Qwen) quantized to fit in 2GB–64GB RAM |

### Why Not Pure Java?
- Pure Java ML frameworks (DL4J, DJL) are too slow for LLM inference and lack support for modern LLM architectures
- The best approach is **Java application ↔ JNI bridge ↔ llama.cpp C++ engine ↔ GGUF model**
- This runs 100% locally, no internet required after initial model download

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    MG-CMS Frontend (React)               │
│                                                          │
│   ┌──────────────────────────────────────────────────┐   │
│   │           Chat Widget (New Component)             │   │
│   │  - Floating chat bubble on all pages              │   │
│   │  - Full chat panel on MG-CMS front page           │   │
│   │  - Uses existing WebSocket (SockJS + STOMP)       │   │
│   └──────────────┬───────────────────────────────────┘   │
│                   │ WebSocket (STOMP)                     │
└───────────────────┼──────────────────────────────────────┘
                    │
┌───────────────────┼──────────────────────────────────────┐
│                   ▼     MG-CMS Backend (Spring Boot)      │
│                                                           │
│   ┌───────────────────────────────────────────────────┐   │
│   │          LlmChatController (NEW)                  │   │
│   │  - REST: POST /api/llm/chat                       │   │
│   │  - REST: GET  /api/llm/status                     │   │
│   │  - REST: POST /api/llm/chat/stream (SSE)          │   │
│   │  - WebSocket: /api/topic/llm-chat                 │   │
│   └──────────────┬────────────────────────────────────┘   │
│                   │                                       │
│   ┌───────────────▼───────────────────────────────────┐   │
│   │          LlmService (NEW)                         │   │
│   │  - Model loading & lifecycle                      │   │
│   │  - Prompt engineering (system prompts per feature) │   │
│   │  - Context management (conversation history)       │   │
│   │  - Token streaming                                 │   │
│   │  - MG-CMS domain knowledge injection               │   │
│   └──────────────┬────────────────────────────────────┘   │
│                   │                                       │
│   ┌───────────────▼───────────────────────────────────┐   │
│   │          LlmInferenceEngine (NEW)                 │   │
│   │  - JNI/JNA bridge to llama.cpp                    │   │
│   │  - Model file management (GGUF)                    │   │
│   │  - RAM-based model selection                       │   │
│   │  - Token generation & sampling                     │   │
│   └──────────────┬────────────────────────────────────┘   │
│                   │ JNI / JNA                             │
└───────────────────┼───────────────────────────────────────┘
                    │
┌───────────────────▼───────────────────────────────────────┐
│              llama.cpp (Native C++ Library)                │
│  - CPU-optimized inference (AVX2, ARM NEON)               │
│  - GGUF model format support                              │
│  - Quantization support (Q2_K to Q8_0)                    │
│  - Memory-mapped file loading                             │
└───────────────────┬───────────────────────────────────────┘
                    │
┌───────────────────▼───────────────────────────────────────┐
│              GGUF Model File (Local Disk)                  │
│  - 2GB tier: TinyLlama 1.1B Q4_K_M (~700MB)              │
│  - 8GB tier: Mistral 7B Q4_K_M (~4.4GB)                  │
│  - 64GB tier: Llama 3 70B Q4_K_M (~40GB)                 │
└───────────────────────────────────────────────────────────┘
```

---

## 3. RAM Tier Strategy

### 🟢 Tier 1: 2 GB RAM (Lightweight / Embedded)

| Attribute | Details |
|---|---|
| **Model** | TinyLlama 1.1B (Q4_K_M quantization) |
| **Model Size on Disk** | ~670 MB |
| **RAM Usage** | ~800 MB – 1.2 GB |
| **Quality** | Basic: simple Q&A, keyword extraction, short summaries |
| **Response Speed** | Fast (2-5 tokens/sec on modern CPU) |
| **Best For** | Simple assistance, form filling help, code lookup |
| **Alternative Models** | Phi-2 (2.7B Q2_K ~1GB), Qwen2 0.5B (~400MB) |

**Capabilities at 2GB:**
- ✅ Basic question answering about MG-CMS features
- ✅ Simple text classification (defect categories, error codes)
- ✅ Form field suggestions
- ⚠️ Limited context window (~2048 tokens)
- ❌ Complex reasoning or multi-step analysis

---

### 🟡 Tier 2: 8 GB RAM (Workstation)

| Attribute | Details |
|---|---|
| **Model** | Mistral 7B Instruct (Q4_K_M quantization) |
| **Model Size on Disk** | ~4.4 GB |
| **RAM Usage** | ~5.5 GB – 6.5 GB |
| **Quality** | Good: complex Q&A, reasoning, document analysis, code generation |
| **Response Speed** | Moderate (3-8 tokens/sec on modern CPU) |
| **Best For** | Full assistant features, report analysis, scheduling suggestions |
| **Alternative Models** | Llama 3 8B (Q4_K_M ~4.7GB), Phi-3 Medium 14B (Q3_K_M ~6.3GB) |

**Capabilities at 8GB:**
- ✅ Intelligent Q&A about cutting plans, production data
- ✅ Scheduling optimization suggestions
- ✅ Quality defect analysis and recommendations
- ✅ Report summarization
- ✅ SQL query generation from natural language
- ✅ Context window ~4096–8192 tokens
- ⚠️ May be slower for very complex reasoning

---

### 🔴 Tier 3: 64 GB RAM (Server)

| Attribute | Details |
|---|---|
| **Model** | Llama 3 70B Instruct (Q4_K_M quantization) |
| **Model Size on Disk** | ~40 GB |
| **RAM Usage** | ~42 GB – 48 GB |
| **Quality** | Excellent: near-GPT-4 level reasoning, code generation, analytics |
| **Response Speed** | Slower (1-4 tokens/sec on CPU) or fast with GPU offload |
| **Best For** | Full AI assistant, advanced analytics, multi-user server |
| **Alternative Models** | Mixtral 8x7B (Q4_K_M ~26GB), Qwen2 72B, DeepSeek 67B |

**Capabilities at 64GB:**
- ✅ Everything from Tier 2, plus:
- ✅ Advanced production optimization reasoning
- ✅ Complex multi-step planning
- ✅ Multi-language support (French, English, Arabic)
- ✅ Detailed report generation
- ✅ Code generation for custom queries
- ✅ Concurrent multi-user support
- ✅ Context window 8192–32768 tokens

---

### 3.1 Model Download Links For The Current Shortlist

Open each model page and use the quantized files or the Quantizations tab for llama.cpp-compatible GGUF downloads.

| Model | Download page | Best use in MG-CMS |
|---|---|---|
| [TinyLlama 1.1B Chat v1.0](https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0) | Hugging Face repo | Legacy 2 GB fallback |
| [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B) | Hugging Face repo | Best 2 GB starter model |
| [Mistral 7B Instruct v0.3](https://huggingface.co/mistralai/Mistral-7B-Instruct-v0.3) | Hugging Face repo | Legacy 8 GB baseline |
| [Qwen3-4B-Instruct-2507](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507) | Hugging Face repo | Best 8 GB main assistant |
| [Phi-4-mini-instruct](https://huggingface.co/microsoft/Phi-4-mini-instruct) | Hugging Face repo | Small English-first fallback |
| [Gemma 3 4B IT](https://huggingface.co/google/gemma-3-4b-it) | Hugging Face repo | Small multilingual fallback |
| [Mistral Small 3.1 24B Instruct](https://huggingface.co/mistralai/Mistral-Small-3.1-24B-Instruct-2503) | Hugging Face repo | Best 64 GB single-model pick |
| [Qwen3-30B-A3B](https://huggingface.co/Qwen/Qwen3-30B-A3B) | Hugging Face repo | Strong 64 GB MoE option |
| [DeepSeek-R1-0528-Qwen3-8B](https://huggingface.co/deepseek-ai/DeepSeek-R1-0528-Qwen3-8B) | Hugging Face repo | Second-pass verifier and reasoning audit model |
| [Meta-Llama-3-70B-Instruct](https://huggingface.co/meta-llama/Meta-Llama-3-70B-Instruct) | Hugging Face repo | Legacy heavyweight 70B-class option |

### 3.2 Updated Recommendation For MG-CMS

| RAM tier | Recommended model | Why |
|---|---|---|
| 2 GB | [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B) | Smallest practical current choice for a local assistant |
| 8 GB | [Qwen3-4B-Instruct-2507](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507) | Best balance of speed, multilingual quality, and agent/tool use |
| 64 GB | [Mistral Small 3.1 24B Instruct](https://huggingface.co/mistralai/Mistral-Small-3.1-24B-Instruct-2503) | Best single-model quality for French-heavy production use |
| 64 GB verifier | [DeepSeek-R1-0528-Qwen3-8B](https://huggingface.co/deepseek-ai/DeepSeek-R1-0528-Qwen3-8B) | Good second-pass reasoning and verification layer |
| 64 GB MoE option | [Qwen3-30B-A3B](https://huggingface.co/Qwen/Qwen3-30B-A3B) | More capacity with only 3.3B active parameters per token |

Operational note for the current MG-CMS implementation: the embedded Java GGUF runtime used by this repository does not yet load Qwen3 architecture models successfully. In practice, the working embedded deployment currently uses TinyLlama for the smallest tier and Mistral 7B Instruct for the main tier, while Qwen3 remains a strategic target for a future runtime upgrade.

## 4. Java Implementation Approach

### 4.1 Maven Dependencies (to add to pom.xml)

```xml
<!-- Option A: llama-java (Java binding for llama.cpp) -->
<dependency>
    <groupId>de.kherud</groupId>
    <artifactId>llama</artifactId>
    <version>3.2.1</version>  <!-- Check for latest version -->
</dependency>

<!-- Option B: Use jlama (Pure Java LLM inference - slower but simpler) -->
<dependency>
    <groupId>com.github.tjake</groupId>
    <artifactId>jlama-core</artifactId>
    <version>0.8.0</version>
</dependency>
```

### 4.2 Key Classes to Create

```
src/main/java/com/lear/MGCMS/
├── controller/
│   └── LlmChatController.java        # REST + WebSocket endpoints
├── services/
│   ├── LlmService.java               # Business logic, prompt engineering
│   └── LlmInferenceEngine.java       # Low-level model interaction
├── domain/
│   ├── LlmChatMessage.java           # Chat message entity
│   ├── LlmChatSession.java           # Conversation session entity
│   └── LlmConfig.java                # Configuration entity
├── payload/
│   ├── LlmChatRequest.java           # Request DTO
│   └── LlmChatResponse.java          # Response DTO
└── repositories/
    ├── LlmChatMessageRepository.java  # Message persistence
    └── LlmChatSessionRepository.java  # Session persistence
```

### 4.3 Configuration (application.properties)

```properties
# LLM Configuration
mgcms.llm.enabled=true
mgcms.llm.model-path=/path/to/models/
mgcms.llm.model-name=mistral-7b-instruct-v0.2.Q4_K_M.gguf
mgcms.llm.ram-tier=8gb
mgcms.llm.context-size=4096
mgcms.llm.max-tokens=512
mgcms.llm.temperature=0.7
mgcms.llm.threads=4
mgcms.llm.gpu-layers=0

# Auto-detect and select model based on available RAM
mgcms.llm.auto-select-model=true
```

### 4.4 LlmService Core Logic (Pseudo-code)

```java
@Service
public class LlmService {

    // On startup: detect available RAM → select appropriate model
    // Load model via llama.cpp JNI bridge
    // Accept chat messages → build prompt with system context
    // Stream tokens back via WebSocket or SSE
    // Persist conversations to MySQL

    // System prompt includes MG-CMS domain knowledge:
    // - Available features and how to use them
    // - Common manufacturing terms
    // - Data schema awareness for query generation
}
```

### 4.5 Frontend Chat Component (React)

```
src/main/js/
├── components/
│   └── LlmChat/
│       ├── ChatWidget.js          # Floating chat bubble
│       ├── ChatPanel.js           # Full chat interface
│       ├── ChatMessage.js         # Individual message component
│       ├── ChatInput.js           # Text input with send button
│       └── ChatSettings.js        # Model selection, preferences
├── actions/
│   └── llmChatActions.js          # Redux actions
└── reducers/
    └── llmChatReducer.js          # Redux reducer
```

---

## 5. Frontend Chat Integration

### 5.1 Chat on MG-CMS Front Page

The chat will be integrated into the existing MG-CMS React frontend:

- **Floating Chat Bubble**: A persistent chat icon (bottom-right corner) visible on all pages
- **Front Page Chat Panel**: A dedicated full-width chat section on the Home/Landing page
- **WebSocket Integration**: Uses the existing WebSocket infrastructure (`/ws` endpoint with SockJS + STOMP)

### 5.2 Streaming Responses

- Token-by-token streaming via WebSocket for real-time typing effect
- Fallback to REST with Server-Sent Events (SSE) if WebSocket unavailable
- Markdown rendering in chat messages for formatted responses

### 5.3 Context-Aware Chat

The LLM will have access to the current page context:
- If on a Cutting Plan page → LLM knows about cutting plan data
- If on Quality Validation → LLM has quality-related context
- If on Scheduling → LLM can assist with scheduling decisions

---

## 6. LLM Recommendations for MG-CMS Features

### 🏭 Where to Use LLM Across ALL MG-CMS Features

#### **A. CUTTING PLAN MANAGEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| Plan Optimization Suggestions | LLM analyzes material usage and suggests better nesting/combinations | 8GB+ |
| Natural Language Plan Creation | "Create a cutting plan for 500 pieces of PN-12345 using reftissu X" | 8GB+ |
| Material Waste Prediction | Analyze historical data to predict material waste per plan | 64GB |
| Anomaly Detection | Flag unusual cutting plans or parameter combinations | 8GB+ |

#### **B. CUTTING REQUEST MANAGEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| Smart Form Filling | Auto-suggest fields based on partial input (PN, reftissu, quantity) | 2GB+ |
| Request Validation | "Is this request feasible given current stock and machine capacity?" | 8GB+ |
| Status Explanations | Explain why a request is in WAITING_MATERIAL status | 2GB+ |
| Priority Recommendations | Suggest request priority based on deadlines and current workload | 8GB+ |

#### **C. SCHEDULING & ORDONNANCEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| Schedule Optimization Chat | "What's the best sequence for today's cutting orders?" | 8GB+ |
| Bottleneck Analysis | Identify and explain production bottlenecks | 8GB+ |
| What-If Scenarios | "What happens if Machine 3 goes down for 2 hours?" | 64GB |
| Natural Language Scheduling | "Schedule PN-456 on Machine 2 after the current series" | 8GB+ |

#### **D. QUALITY MANAGEMENT (Audit, First Check, Validation)**
| Use Case | Description | RAM Tier |
|---|---|---|
| Defect Classification | Auto-classify defects from text descriptions | 2GB+ |
| Root Cause Analysis | Suggest potential root causes for recurring defects | 8GB+ |
| Quality Report Summarization | Summarize quality reports in natural language | 8GB+ |
| Corrective Action Suggestions | Recommend corrective actions based on defect patterns | 8GB+ |
| Quality Notice Drafting | Auto-generate quality notice documents | 8GB+ |

#### **E. MACHINE MANAGEMENT (CNC, Laser, Cutting)**
| Use Case | Description | RAM Tier |
|---|---|---|
| Machine Status Explanations | "Why is Machine CNC-05 showing code ARRET-07?" | 2GB+ |
| Predictive Maintenance Chat | "When should Machine 3 be scheduled for maintenance?" | 8GB+ |
| Error Code Lookup | Natural language search for error codes and solutions | 2GB+ |
| Performance Analysis | "Compare Machine 1 and Machine 2 performance this week" | 8GB+ |
| CNC Program Assistance | Help with CNC program parameters and settings | 64GB |

#### **F. INVENTORY & MATERIAL MANAGEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| Stock Level Alerts | "Which materials are running low for next week's orders?" | 8GB+ |
| Material Demand Forecasting Chat | Explain demand forecasts in natural language | 8GB+ |
| Reftissu Selection | "Which reftissu is best for PN-789 considering cost and availability?" | 8GB+ |
| Inventory Discrepancy Analysis | Explain stock discrepancies and suggest corrections | 8GB+ |

#### **G. PRODUCTION TRACKING & REPORTING**
| Use Case | Description | RAM Tier |
|---|---|---|
| KPI Explanation | "Explain why IPPM increased this week" | 8GB+ |
| Report Generation | "Generate a daily production summary for Zone A" | 8GB+ |
| Natural Language Queries | "How many pieces of PN-123 were cut yesterday?" → SQL query | 8GB+ |
| Dashboard Insights | AI-generated insights on dashboard data | 8GB+ |
| Coupe Performance Analysis | Analyze cutting performance trends | 8GB+ |

#### **H. PART NUMBER & BOM MANAGEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| BOM Validation | "Check if the BOM for PN-123 is complete and consistent" | 8GB+ |
| Weight Calculation Assistance | Explain CAD weight calculations, flag anomalies | 2GB+ |
| Part Number Search | Natural language part number lookup | 2GB+ |
| Material Config Suggestions | Suggest material configurations based on similar PNs | 8GB+ |

#### **I. GAMME TECHNIQUE (Technical Specifications)**
| Use Case | Description | RAM Tier |
|---|---|---|
| Spec Explanation | Explain technical specifications in simple language | 2GB+ |
| Spec Comparison | Compare specs between two part numbers or versions | 8GB+ |
| Gamme Drafting | Help draft new technical specifications | 8GB+ |

#### **J. USER ASSISTANCE & TRAINING**
| Use Case | Description | RAM Tier |
|---|---|---|
| Feature Discovery | "How do I create a cutting plan?" → step-by-step guide | 2GB+ |
| Error Resolution | "I got error X, what should I do?" | 2GB+ |
| Onboarding Assistant | Guide new users through MG-CMS features | 2GB+ |
| Multi-language Support | Assist in French, English, and Arabic | 8GB+ |

#### **K. NOTIFICATION SYSTEM ENHANCEMENT**
| Use Case | Description | RAM Tier |
|---|---|---|
| Smart Notification Summaries | Summarize multiple notifications into actionable insights | 2GB+ |
| Notification Priority | AI-prioritize notifications based on context | 8GB+ |
| Alert Explanation | "What does this CAD material mismatch notification mean?" | 2GB+ |

#### **L. PLS (LAMINATION) MODULE**
| Use Case | Description | RAM Tier |
|---|---|---|
| Scrap Analysis | "Why is scrap rate high for this reftissu?" | 8GB+ |
| Demand Forecasting | Natural language demand forecasts | 8GB+ |
| Process Optimization | Lamination process improvement suggestions | 8GB+ |

#### **M. QUERY BUILDER (QueryController)**
| Use Case | Description | RAM Tier |
|---|---|---|
| Natural Language to SQL | "Show me all cutting plans from last week with waste > 10%" → SQL | 8GB+ |
| Query Explanation | Explain what a complex query does in plain language | 8GB+ |
| Report Builder | Build custom reports through conversational interface | 8GB+ |

---

## 7. Implementation Phases

### Phase 1: Foundation (2-3 weeks)
- [ ] Set up llama.cpp Java binding (llama-java dependency)
- [ ] Create LlmInferenceEngine with model loading/inference
- [ ] Create LlmService with basic chat functionality
- [ ] Create LlmChatController (REST endpoints)
- [ ] Auto-detect RAM and select appropriate model
- [ ] Test with TinyLlama (2GB tier) as default

### Phase 2: Frontend Chat (1-2 weeks)
- [ ] Create ChatWidget React component (floating bubble)
- [ ] Create ChatPanel for the Home/Landing page
- [ ] Integrate with existing WebSocket infrastructure
- [ ] Token streaming via STOMP WebSocket
- [ ] Chat history persistence (MySQL)
- [ ] Redux state management for chat

### Phase 3: MG-CMS Domain Knowledge (2-3 weeks)
- [ ] Create system prompts with MG-CMS domain knowledge
- [ ] Page-aware context injection (current feature awareness)
- [ ] Feature-specific prompt templates (cutting, quality, scheduling)
- [ ] Database schema awareness for query generation
- [ ] User role-based prompt customization (admin vs operator)

### Phase 4: Advanced Features (3-4 weeks)
- [ ] Natural Language to SQL for QueryController
- [ ] Quality defect classification integration
- [ ] Scheduling assistant integration
- [ ] Report summarization features
- [ ] Multi-language support (French, English, Arabic)

### Phase 5: Optimization & Scaling (2-3 weeks)
- [ ] Model caching and memory optimization
- [ ] Request queuing for multi-user support
- [ ] Response caching for common questions
- [ ] GPU offload support (optional, for 64GB server tier)
- [ ] Admin panel for LLM configuration
- [ ] Model download/update management UI

---

## 8. Questions

Before proceeding with implementation, I need your input on the following:

### Priority & Scope
1. **Which RAM tier do you want to start with?** (2GB for quick testing, 8GB for good quality, or 64GB for best quality?) i want to be able to use all of them but for start i will start using the 2 GB model thne increase to 8 or 64 when i will launch it on my server.
2. **Which MG-CMS features should the LLM prioritize first?** (e.g., cutting plan assistance, quality, scheduling, general help?) usually we can have some question the user can ask it and he will try understand why something is happenning , and based also on the role he can also either do direct queries into the server like insert or delete and also i want to also use in the Adnvanced orderin feature Advanced_Ordonancement.md if it could help somehow.
3. **Should the chat be visible on ALL pages or only on the Home/Landing page initially?**
i want it to be like it is in a fancy website with a fixed button on bottom right and when click on it we can be able to chat with it
### Technical Decisions
4. **Java version upgrade**: The project uses Java 16. Some LLM libraries work best with Java 17+. Are you open to upgrading to Java 17 or 21? you can upgrade to 17
5. **Model storage**: Where should the GGUF model files be stored? (e.g., `/opt/mgcms/models/`, or configurable via application.properties?) let it be in all applciation properties in C:\CMS\models
6. **Chat history**: Should conversations be persisted to the MySQL database, or is in-memory (session-only) sufficient? i thing you can leave a 7 day log in the folder as a text log . aldo have it configurable in application.properties put the for now the folder C:\CMS\modelsLog
7. **Multi-user**: Should each user have their own chat session, or is a shared chat acceptable? each user have their own chat session and also the closing auto mati

### Language & Content
8. **Primary language**: Should the LLM respond in French, English, or auto-detect from the user's message? auto-detect but it will be either english or fresh  nothing more
9. **MG-CMS documentation**: Do you have existing user documentation or guides that could be fed to the LLM as knowledge base? the only documentation that i have are in the md folder but there is more that that
10. **Data access**: Should the LLM be able to query the database directly (read-only) to answer questions about production data? yes

### Deployment
11. **Deployment environment**: Is MG-CMS deployed on Windows or Linux servers? windows
12. **Concurrent users**: How many users would use the chat simultaneously? may be 5
13. **Do you want the LLM to start automatically with MG-CMS, or should it be a manually-started optional feature?** automatically start but could be disable or enable in the application.properties

---

## 📊 Summary

| Aspect | Answer |
|---|---|
| **Is it possible?** | ✅ Yes |
| **Pure Java?** | Partially — Java app + native llama.cpp via JNI |
| **Runs locally?** | ✅ Yes, 100% offline after model download |
| **2GB RAM?** | ✅ Yes — [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B) (TinyLlama is the legacy fallback) |
| **8GB RAM?** | ✅ Yes — [Qwen3-4B-Instruct-2507](https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507) |
| **64GB RAM?** | ✅ Yes — [Mistral Small 3.1 24B Instruct](https://huggingface.co/mistralai/Mistral-Small-3.1-24B-Instruct-2503) or [Qwen3-30B-A3B](https://huggingface.co/Qwen/Qwen3-30B-A3B) |
| **Chat on front page?** | ✅ Yes — React widget + WebSocket streaming |
| **Effort estimate** | ~10-15 weeks for full implementation |
| **Key dependency** | `de.kherud:llama` (Java binding for llama.cpp) |
