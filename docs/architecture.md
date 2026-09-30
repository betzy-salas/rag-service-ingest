# Arquitectura de confluence-ingestion-service

Diagramas del diseño actual: C4 (contexto, contenedores y componentes), secuencia de las
operaciones y clases. Están en Mermaid, así que se ven directamente en GitHub o GitLab.

## C4 nivel 1: contexto

```mermaid
flowchart LR
  operator["👤 Operador<br/><small>Lanza ingestas y sincronizaciones</small>"]
  service["<b>confluence-ingestion-service</b><br/><small>Spring Boot · Java 21<br/>Convierte páginas de Confluence en chunks con embeddings</small>"]
  confluence["Confluence Cloud<br/><small>API REST v2</small>"]
  openai["OpenAI<br/><small>Modelo de embeddings</small>"]
  db[("PostgreSQL + pgvector<br/><small>rag-service-db</small>")]
  consul["Consul<br/><small>Configuración y secretos</small>"]
  assistant["assistant<br/><small>Servicio de consulta RAG</small>"]

  operator -- "HTTP: POST /ingestions,<br/>POST /synchronizations, GET /pages" --> service
  service -- "Lee espacios y páginas<br/>(Basic auth)" --> confluence
  service -- "Genera embeddings" --> openai
  service -- "Guarda chunks y páginas indexadas" --> db
  service -- "Lee config/assistant/" --> consul
  assistant -- "Busca chunks por similitud" --> db
  assistant -- "Lee config/assistant/" --> consul
```

## C4 nivel 2: contenedores

```mermaid
flowchart LR
  operator["👤 Operador"]

  subgraph svc["confluence-ingestion-service · puerto 8082"]
    api["API REST<br/><small>Spring MVC</small>"]
    core["Ingesta y sincronización<br/><small>Spring AI · JPA · RestClient</small>"]
    liquibase["Liquibase<br/><small>Migraciones al arrancar</small>"]
  end

  subgraph pg["PostgreSQL + pgvector · rag-service-db"]
    chunk[("confluence_chunk<br/><small>content · metadata jsonb · embedding vector(3072)</small>")]
    page[("confluence_page<br/><small>page_id · space_key · title · version · chunk_count · indexed_at</small>")]
  end

  consul["Consul KV<br/><small>config/assistant/*</small>"]
  confluence["Confluence Cloud"]
  openai["OpenAI"]

  operator --> api --> core
  core -- "RestClient" --> confluence
  core -- "EmbeddingModel" --> openai
  core -- "VectorStore" --> chunk
  core -- "JPA" --> page
  liquibase -- "crea tablas" --> pg
  svc -. "configuración al arrancar" .-> consul
```

## C4 nivel 3: componentes

```mermaid
flowchart TB
  subgraph controller["controller"]
    ingestionController["IngestionController<br/><small>POST /ingestions · POST /synchronizations</small>"]
    pagesController["PagesController<br/><small>GET /pages</small>"]
    handler["GlobalExceptionHandler<br/><small>errores → ProblemDetail</small>"]
  end

  subgraph service["service"]
    ingestionService["IngestionService"]
    synchronizationService["SynchronizationService"]
    pageService["PageService"]
  end

  subgraph common["common"]
    pageIndexer["PageIndexer<br/><small>secciones → chunks → embeddings</small>"]
    sectionSplitter["HtmlSectionSplitter<br/><small>HTML → una sección por encabezado</small>"]
  end

  subgraph client["client"]
    confluenceClient["ConfluenceClient"]
  end

  subgraph persistence["repository / Spring AI"]
    repository["ConfluencePageRepository<br/><small>JPA</small>"]
    vectorStore["VectorStore<br/><small>PgVectorStore</small>"]
  end

  ingestionController --> ingestionService
  ingestionController --> synchronizationService
  pagesController --> pageService
  ingestionService --> confluenceClient
  ingestionService --> pageIndexer
  synchronizationService --> confluenceClient
  synchronizationService --> repository
  synchronizationService --> pageIndexer
  pageService --> confluenceClient
  pageIndexer --> sectionSplitter
  pageIndexer --> vectorStore
  pageIndexer --> repository
```

Cada capa tiene sus propios modelos y mappers (MapStruct): `client/dto` para el JSON de
Confluence, `common/model` para lo que comparten client y services, `service/model` para lo que
devuelven los services y `controller/dto` para las respuestas de la API.

## Secuencia: ingesta completa

`POST /ingestions?spaceKey=DS` indexa todas las páginas del espacio, hayan cambiado o no.

```mermaid
sequenceDiagram
  actor Op as Operador
  participant C as IngestionController
  participant S as IngestionService
  participant CL as ConfluenceClient
  participant CF as Confluence
  participant PI as PageIndexer
  participant HS as HtmlSectionSplitter
  participant VS as VectorStore
  participant AI as OpenAI
  participant R as ConfluencePageRepository

  Op->>C: POST /ingestions?spaceKey=DS
  C->>S: ingest("DS")
  S->>CL: listPages("DS")
  CL->>CF: GET /wiki/api/v2/spaces?keys=DS
  CF-->>CL: id del espacio
  loop tandas de 250 (siguiendo _links.next)
    CL->>CF: GET /spaces/{id}/pages?body-format=storage
    CF-->>CL: páginas con cuerpo
  end
  CL-->>S: List<ConfluencePage>
  loop cada página
    S->>PI: index("DS", page)
    PI->>VS: delete(pageId)
    PI->>HS: split(html)
    HS-->>PI: secciones (una por encabezado)
    PI->>PI: "Título > Encabezado" + texto de cada sección
    PI->>PI: TokenTextSplitter (300 tokens) para secciones largas
    PI->>VS: add(chunks)
    VS->>AI: embeddings
    PI->>R: save(ConfluencePageEntity)
  end
  S-->>C: IngestionSummary
  C-->>Op: 200 {"pages":4,"chunks":9}
```

## Secuencia: sincronización

`POST /synchronizations?spaceKey=DS` solo descarga e indexa las páginas nuevas o con otra versión.

```mermaid
sequenceDiagram
  actor Op as Operador
  participant C as IngestionController
  participant S as SynchronizationService
  participant R as ConfluencePageRepository
  participant CL as ConfluenceClient
  participant CF as Confluence
  participant PI as PageIndexer

  Op->>C: POST /synchronizations?spaceKey=DS
  C->>S: synchronize("DS")
  S->>R: findBySpaceKey("DS")
  R-->>S: versiones guardadas (pageId → version)
  S->>CL: listPageVersions("DS")
  CL->>CF: GET /spaces/{id}/pages (sin cuerpo)
  CF-->>CL: id, título y versión
  CL-->>S: List<ConfluencePageVersion>
  S->>S: PageChange.of(...) → NEW · UPDATED · UNCHANGED
  loop páginas NEW y UPDATED
    S->>CL: getPage(pageId)
    CL->>CF: GET /wiki/api/v2/pages/{id}?body-format=storage
    CF-->>CL: página con cuerpo
    S->>PI: index("DS", page)
  end
  Note over S: UNCHANGED: no se descargan ni se embeben.<br/>Las páginas borradas en Confluence no se eliminan.
  S-->>C: SynchronizationSummary
  C-->>Op: 200 {"newPages":1,"updatedPages":0,"unchangedPages":5,"chunks":2}
```

## Secuencia: errores

```mermaid
sequenceDiagram
  actor Op as Operador
  participant C as Controller
  participant S as Service / Client
  participant H as GlobalExceptionHandler

  Op->>C: petición
  C->>S: operación
  S--xC: excepción
  C->>H: excepción
  alt SpaceNotFoundException
    H-->>Op: 404 ProblemDetail
  else Confluence 401 / 403
    H-->>Op: 502 "Confluence rejected the credentials"
  else Otro error de Confluence (RestClientException)
    H-->>Op: 502 "Could not retrieve information from Confluence"
  else Error de OpenAI (OpenAIException)
    H-->>Op: 502 "Could not generate embeddings with OpenAI"
  else Falta spaceKey
    H-->>Op: 400 ProblemDetail
  else Cualquier otro
    H-->>Op: 500 "Internal service error"
  end
```

## Diagrama de clases

```mermaid
classDiagram
  direction LR

  class IngestionController {
    +ingest(spaceKey) ResponseEntity~IngestionResult~
    +synchronize(spaceKey) ResponseEntity~SynchronizationResult~
  }
  class PagesController {
    +listPages(spaceKey) ResponseEntity~List~PageSummary~~
  }
  class GlobalExceptionHandler {
    <<RestControllerAdvice>>
  }

  class IngestionService {
    +ingest(spaceKey) IngestionSummary
  }
  class SynchronizationService {
    +synchronize(spaceKey) SynchronizationSummary
    -storedVersions(spaceKey) Map
  }
  class PageService {
    +listPages(spaceKey) List~PageInfo~
  }
  class PageIndexer {
    +index(spaceKey, ConfluencePage) int
    -withTitle(title, Section)$ String
  }
  class HtmlSectionSplitter {
    +split(html) List~Section~
  }
  class Section {
    <<record>>
    heading
    text
  }
  class ConfluenceClient {
    +listPages(spaceKey) List~ConfluencePage~
    +listPageVersions(spaceKey) List~ConfluencePageVersion~
    +getPage(pageId) ConfluencePage
  }
  class ConfluencePageRepository {
    <<JpaRepository>>
    +findBySpaceKey(spaceKey) List~ConfluencePageEntity~
  }
  class VectorStore {
    <<Spring AI>>
  }
  class ConfluenceProperties {
    <<record>>
    baseUrl
    email
    apiToken
    +wikiUrl() String
  }

  class ConfluencePage {
    <<record>>
    id
    title
    version
    body
    url
  }
  class ConfluencePageVersion {
    <<record>>
    id
    title
    version
  }
  class ConfluencePageEntity {
    <<Entity>>
    pageId
    spaceKey
    title
    version
    chunkCount
    indexedAt
  }
  class PageChange {
    <<enum>>
    NEW
    UPDATED
    UNCHANGED
    +of(page, storedVersions)$ PageChange
  }

  IngestionController --> IngestionService
  IngestionController --> SynchronizationService
  PagesController --> PageService
  IngestionService --> ConfluenceClient
  IngestionService --> PageIndexer
  SynchronizationService --> ConfluenceClient
  SynchronizationService --> ConfluencePageRepository
  SynchronizationService --> PageIndexer
  SynchronizationService ..> PageChange
  PageService --> ConfluenceClient
  PageIndexer --> HtmlSectionSplitter
  HtmlSectionSplitter ..> Section
  PageIndexer --> VectorStore
  PageIndexer --> ConfluencePageRepository
  ConfluenceClient --> ConfluenceProperties
  ConfluenceClient ..> ConfluencePage
  ConfluenceClient ..> ConfluencePageVersion
  ConfluencePageRepository ..> ConfluencePageEntity
```

Para no sobrecargar el diagrama se omiten los mappers de MapStruct, los DTOs de cada capa y las
clases de constantes (`ApiPaths`, `ConfluenceApi`, `ChunkMetadata`, `ErrorMessages`).
