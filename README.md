CodeLens
Backend-focused REST API for semantic code intelligence over Java repositories.
CodeLens clones a public GitHub repository, parses its Java source code, enriches each method with an LLM-generated summary and a vector embedding, stores the enriched method documents in Elasticsearch, and exposes REST APIs for semantic code search, code explanation, and multi-turn AI chat about indexed methods.
> This is primarily a **backend engineering project**. The React frontend is a lightweight demo client used to exercise and showcase the API.
---
Table of Contents
Overview
Why This Project Exists
Tech Stack
Key Features
Backend Architecture
API Overview
Elasticsearch Data Model
Authentication and Security
Frontend Demo Client
Local Setup
Configuration
Testing
Engineering Decisions
Known Limitations
Future Improvements
Reviewer Summary
---
Overview
A developer submits a GitHub repository URL. CodeLens then:
Clones the repository using JGit.
Recursively scans Java source files.
Parses `.java` files with JavaParser to extract classes, methods, signatures, fields, line ranges, and method bodies.
Dumps parsed file structures to JSON for debugging and auditability.
Runs an asynchronous enrichment pipeline:
summarizes methods with the HuggingFace Inference API,
generates 768-dimensional embeddings,
indexes enriched method documents into Elasticsearch.
Provides backend APIs for:
semantic method search,
class-scoped search,
single-shot method explanation,
multi-turn chat sessions anchored to a method.
---
Why This Project Exists
CodeLens was built to practice backend engineering beyond simple CRUD APIs.
The main engineering goals were:
Build a multi-stage backend pipeline: clone → scan → parse → summarize → embed → index.
Practice REST API design with DTOs, meaningful status codes, and structured responses.
Integrate LLM services into backend workflows while handling failures gracefully.
Use Elasticsearch vector search for semantic retrieval over code.
Implement asynchronous processing with progress tracking and conflict prevention.
Add API key authentication at the servlet filter level before controller logic runs.
Keep the frontend intentionally small so the backend remains the focus.
---
Tech Stack
Layer	Technology
Backend	Java 21, Spring Boot 4.0.0, Spring MVC
Search / Storage	Elasticsearch 8.x, Spring Data Elasticsearch
Code Parsing	JavaParser 3.27.1, JavaParser Symbol Solver
Git Integration	JGit 6.9.0
LLM / Embeddings	HuggingFace Inference API
Chat Model	Qwen2.5-Coder-7B-Instruct
Embedding Model	BAAI/bge-base-en-v1.5
Security	API key auth with `X-API-Key` header
Async / Scheduling	Spring `@Async`, `ThreadPoolTaskExecutor`, `@Scheduled`
Serialization	Jackson `ObjectMapper`
Build	Maven Wrapper
Frontend Demo	React 18, Vite 5, lucide-react
---
Key Features
Repository Ingestion
Clone public GitHub repositories over HTTPS.
Use shallow cloning to reduce disk and network overhead.
Recursively scan Java source files.
Skip irrelevant directories such as `.git`, `target`, and hidden folders.
Parse Java source into structured representations.
Static Code Parsing
Extract classes, methods, signatures, return types, parameters, fields, method bodies, and line ranges.
Store parsed structure as JSON files for transparency and debugging.
Keep parser concerns isolated from indexing and search logic.
Asynchronous Enrichment Pipeline
Runs in the background using Spring `@Async`.
Returns `202 Accepted` immediately when enrichment starts.
Tracks:
total files,
processed files,
percentage,
current status.
Prevents concurrent pipeline runs with a `409 Conflict` response.
Retries failed files once after a short delay.
Batches methods to reduce HuggingFace API calls.
Semantic Search
Converts a user query into an embedding.
Searches Elasticsearch using cosine similarity over dense vectors.
Falls back to BM25 full-text search when embedding generation fails.
Supports pagination and class-scoped filtering.
Code Explanation
Explain a method by Elasticsearch document ID.
Explain a method by class name and method name.
Sends method body and class context to the LLM.
Returns a focused explanation for the selected method.
Multi-Turn Code Chat
Starts a chat session anchored to a specific method.
Keeps message history for follow-up questions.
Uses an in-memory `ConcurrentHashMap` session store.
Expires inactive sessions after 30 minutes.
Cleans up stale sessions every 10 minutes with `@Scheduled`.
API Security
All API endpoints require `X-API-Key`.
Missing API key returns `401 Unauthorized`.
Invalid API key returns `403 Forbidden`.
CORS preflight requests are allowed.
`/actuator/health` and `/error` are exempted.
---
Backend Architecture
```text
HTTP Request
   |
   v
ApiKeyAuthFilter
   |
   v
Controller Layer
   |
   v
Service Layer
   |
   v
Repository Layer
   |
   v
Elasticsearch
```
Main Backend Components
Component	Responsibility
`ApiKeyAuthFilter`	Validates `X-API-Key` before controller logic
`IngestionController`	Repository download and enrichment endpoints
`SearchController`	Search, explanation, and chat endpoints
`GitService`	Clones repositories and starts scanning
`JavaSourceScannerService`	Finds Java files recursively
`JavaParserService`	Parses Java source into structured models
`JsonDumpService`	Writes parsed output to JSON files
`EnrichmentService`	Async summarization, embedding, retry, and indexing pipeline
`HuggingFaceService`	Calls HuggingFace chat and embedding APIs
`ElasticsearchIndexingService`	Bulk indexes enriched method documents
`CodeSearchService`	Semantic search, BM25 fallback, explanation, and chat logic
`ChatSessionStore`	In-memory session store with TTL eviction
`CodeEntityRepository`	Spring Data Elasticsearch repository
Project Structure
```text
CodeLens/
├── CodeLens/                         # Spring Boot backend
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/CodeLens/
│       │   │   ├── CodeLensApplication.java
│       │   │   ├── config/
│       │   │   │   ├── AsyncConfig.java
│       │   │   │   └── CorsConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── IngestionController.java
│       │   │   │   └── SearchController.java
│       │   │   ├── dto/
│       │   │   ├── exception/
│       │   │   ├── filter/
│       │   │   │   └── ApiKeyAuthFilter.java
│       │   │   ├── model/
│       │   │   │   ├── ParsedClass.java
│       │   │   │   ├── ParsedJavaFile.java
│       │   │   │   ├── ParsedMethod.java
│       │   │   │   └── elastic_model/
│       │   │   │       └── CodeEntityDoc.java
│       │   │   ├── chat/
│       │   │   ├── ElasticSearch/
│       │   │   │   └── repository/
│       │   │   │       └── CodeEntityRepository.java
│       │   │   └── service/
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           └── java/com/example/CodeLens/
│
└── front/                            # Lightweight React demo UI
    ├── src/
    │   ├── api/
    │   ├── components/
    │   └── hooks/
    └── vite.config.js
```
---
API Overview
All API requests, except health/error endpoints, require:
```http
X-API-Key: your-api-key
```
Ingestion API
Method	Endpoint	Description
`POST`	`/api/repo/download`	Clone a GitHub repository, parse Java files, and dump JSON
`POST`	`/api/repo/enrich-data`	Start async summarization, embedding, and indexing
`GET`	`/api/repo/enrich-status`	Get current enrichment progress
Example: Download Repository
```json
{
  "url": "https://github.com/owner/repo"
}
```
Example: Enrichment Status
```json
{
  "running": true,
  "status": "IN_PROGRESS",
  "total_files": 42,
  "processed_files": 17,
  "percentage": 40
}
```
Search API
Method	Endpoint	Description
`POST`	`/api/search`	Semantic search with BM25 fallback
`POST`	`/api/search/by-class`	Search within a specific class
`POST`	`/api/search/explain`	Explain a method by document ID
`POST`	`/api/search/explain/by-class`	Explain a method by class and method name
Example: Semantic Search
```json
{
  "query": "parse java source file",
  "topK": 20,
  "page": 0,
  "size": 10
}
```
Example: Explain Method
```json
{
  "methodId": "elasticsearch-document-id",
  "question": "Explain this method step by step."
}
```
Chat API
Method	Endpoint	Description
`POST`	`/api/chat/start`	Start a method-anchored chat session
`POST`	`/api/chat/turn`	Continue an existing chat session
Example: Start Chat
```json
{
  "methodId": "elasticsearch-document-id",
  "question": "What does this method do?"
}
```
Example: Continue Chat
```json
{
  "sessionId": "session-uuid",
  "question": "Why does it return null in this case?"
}
```
---
Elasticsearch Data Model
CodeLens uses Elasticsearch as its primary data store.
Index: `code-entities`
Field	Type	Description
`id`	keyword	Elasticsearch document ID
`filePath`	keyword	Relative source file path
`className`	keyword	Java class name
`methodName`	keyword	Java method name
`methodBody`	text	Full method source code
`summary`	text	LLM-generated method summary
`embedding`	dense_vector	768-dimensional vector for semantic search
The vector field enables cosine similarity search. The text fields support BM25 fallback search.
Minimal Vector Mapping
```json
{
  "mappings": {
    "properties": {
      "embedding": {
        "type": "dense_vector",
        "dims": 768
      }
    }
  }
}
```
---
Authentication and Security
CodeLens uses API key authentication.
API Key Flow
Client sends `X-API-Key`.
`ApiKeyAuthFilter` checks the key before any controller logic.
Missing key returns `401`.
Wrong key returns `403`.
Valid key allows the request to continue.
CORS
CORS is configured in both:
the servlet filter,
a `WebMvcConfigurer`.
This ensures frontend demo requests and preflight `OPTIONS` requests work correctly.
Secret Handling
For local development, use placeholder values in `application.properties`.
Before any public GitHub push or deployment:
Do not commit real HuggingFace API keys.
Do not commit real API keys.
Prefer environment variables or a local ignored config file.
Add a `.env.example` with placeholder values.
---
Frontend Demo Client
The `front/` directory contains a small React + Vite demo UI. It is included only to demonstrate and test the backend API.
It includes:
repository ingestion modal,
search bar,
code result cards,
method explanation modal,
chat drawer,
enrichment progress polling.
The Vite dev server proxies `/api/*` requests to:
```text
http://localhost:8081
```
The API client is located at:
```text
front/src/api/codelens.js
```
---
Local Setup
Prerequisites
Java 21
Maven or Maven Wrapper
Elasticsearch 8.x running on `localhost:9200`
HuggingFace Inference API key
Node.js 18+ and npm for the demo frontend
1. Start Elasticsearch
Run Elasticsearch 8.x locally.
The backend expects:
```properties
spring.elasticsearch.uris=http://localhost:9200
```
Create the `code-entities` index with the vector mapping if automatic creation is disabled.
2. Configure Backend
Edit:
```text
CodeLens/src/main/resources/application.properties
```
Use placeholder/local values:
```properties
server.port=8081

spring.elasticsearch.uris=http://localhost:9200

huggingface.api.key=YOUR_HUGGINGFACE_API_KEY
huggingface.model.chat=Qwen/Qwen2.5-Coder-7B-Instruct
huggingface.model.embedding=BAAI/bge-base-en-v1.5

codelens.api.key=YOUR_STRONG_API_KEY

app.ingestion.work-dir=codelens_data/repos
```
3. Run Backend
```bash
cd CodeLens/CodeLens

./mvnw spring-boot:run
```
On Windows:
```powershell
cd CodeLens\CodeLens

.\mvnw.cmd spring-boot:run
```
The API runs on:
```text
http://localhost:8081
```
4. Run Demo Frontend
```bash
cd front

npm install
npm run dev
```
Frontend runs on:
```text
http://localhost:5173
```
---
Configuration
Property	Example	Description
`server.port`	`8081`	Backend HTTP port
`spring.elasticsearch.uris`	`http://localhost:9200`	Elasticsearch URL
`huggingface.api.key`	`YOUR_HUGGINGFACE_API_KEY`	HuggingFace Inference API key
`huggingface.model.chat`	`Qwen/Qwen2.5-Coder-7B-Instruct`	Chat/explanation model
`huggingface.model.embedding`	`BAAI/bge-base-en-v1.5`	Embedding model
`codelens.api.key`	`YOUR_STRONG_API_KEY`	Required value for `X-API-Key`
`app.ingestion.work-dir`	`codelens_data/repos`	Local clone and JSON dump directory
---
Testing
Run backend tests:
```bash
cd CodeLens/CodeLens
./mvnw test
```
Windows:
```powershell
cd CodeLens\CodeLens
.\mvnw.cmd test
```
Current test coverage is minimal and mainly verifies Spring context wiring. Service-level and integration tests are planned.
---
Engineering Decisions
Why Elasticsearch?
The main query type is semantic similarity over vector embeddings, combined with text search fallback. Elasticsearch provides both dense vector search and BM25 retrieval in one storage layer.
Why JavaParser?
JavaParser provides structured AST parsing without compiling the target project. This makes it suitable for analyzing arbitrary GitHub repositories.
Why Asynchronous Enrichment?
Summarization and embedding calls are slow and network-bound. Running them asynchronously keeps the API responsive and allows progress polling.
Why API Key Authentication?
For a backend demo and internal tool, API key auth is simple, predictable, and easy to test. It keeps the project focused on the ingestion/search pipeline instead of user account management.
Why DTOs?
DTOs make request and response contracts explicit and prevent controller methods from exposing internal models directly.
Why BM25 Fallback?
External LLM/embedding APIs can fail, time out, or hit rate limits. BM25 fallback keeps search usable even when embeddings are unavailable.
---
Known Limitations
Only Java parsing is currently wired.
Python scanning exists only as a scaffold and is not connected to the pipeline.
Chat sessions are in-memory and disappear after restart.
No Redis or persistent session storage.
Minimal automated test coverage.
No Swagger/OpenAPI documentation yet.
No Docker Compose setup for backend + Elasticsearch.
Elasticsearch index mapping may need to be created manually.
API key is suitable for demo/internal use, not a complete production auth system.
The React UI is a demo client, not a full product frontend.
---
Future Improvements
Add `.env.example` and move secrets out of `application.properties`.
Add Docker Compose for Elasticsearch + backend.
Add Swagger/OpenAPI documentation.
Add unit tests for:
`EnrichmentService`,
`CodeSearchService`,
`HuggingFaceService`,
`ApiKeyAuthFilter`,
`ChatSessionStore`.
Add integration tests with Testcontainers for Elasticsearch.
Replace in-memory chat sessions with Redis.
Add per-API-key rate limiting.
Wire Python parsing into the ingestion pipeline.
Add structured logging with request IDs.
Add GitHub Actions CI.
Externalize embedding dimensions as a config property.
---
Reviewer Summary
CodeLens demonstrates backend engineering around a realistic AI-assisted developer tool. It combines repository ingestion, static Java parsing, LLM summarization, vector embedding generation, Elasticsearch semantic search, method-level explanation, multi-turn chat sessions, API key security, asynchronous processing, and a small React demo client.
The project shows practical backend skills in REST API design, service-layer decomposition, Elasticsearch modeling, external API integration, async workflow management, and defensive fallback handling. It is best understood as a backend-first portfolio project, with the frontend included only as a demonstration layer.
