# confluence-ingestion-service

Servicio que lee las páginas de un espacio de **Confluence Cloud**, las divide en fragmentos
(chunks), genera sus embeddings con **OpenAI** y los guarda en **PostgreSQL + pgvector**. Esos
chunks son la base de conocimiento que consulta el servicio RAG (`assistant`) para responder
preguntas citando las páginas de origen.

Es una prueba de concepto: una sola instancia y espacios de hasta unas 50 páginas.

## Qué hace

| Operación | Endpoint | Descripción |
|---|---|---|
| Listar páginas | `GET /pages?spaceKey=DS` | Devuelve id, título y versión de cada página del espacio, sin descargar su contenido. |
| Ingesta completa | `POST /ingestions?spaceKey=DS` | Indexa **todas** las páginas del espacio, hayan cambiado o no. |
| Sincronización | `POST /synchronizations?spaceKey=DS` | Indexa solo las páginas **nuevas o con otra versión**. Las demás no se descargan ni se vuelven a embeber. |

Indexar una página significa:
1. Dividir su HTML (formato *storage* de Confluence) en secciones, una por encabezado, con una
   línea por bloque de texto.
2. Convertir cada sección en un chunk que empieza con el título de la página y el encabezado
   (`Título > Encabezado`). Las secciones de más de 300 tokens se dividen con el
   `TokenTextSplitter` de Spring AI. Una página entera en un solo chunk no superaba el umbral de
   similitud del servicio de consulta para ninguna pregunta concreta.
3. Borrar los chunks anteriores de esa página y guardar los nuevos con su embedding en
   `confluence_chunk`.
4. Registrar la página y su versión en `confluence_page`, que es lo que usa la sincronización para
   saber qué cambió.

Cada chunk lleva en su metadata `pageId`, `spaceKey`, `title` y `url`. `title` y `url` son las
fuentes que el servicio de consulta muestra en sus respuestas.

Los diagramas C4, de secuencia y de clases están en [docs/architecture.md](docs/architecture.md).

## Tecnologías

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Data JPA, Actuator) |
| IA | Spring AI 2.0.1: OpenAI (embeddings), PgVectorStore, `TokenTextSplitter` |
| HTML | Jsoup (secciones por encabezado en `HtmlSectionSplitter`) |
| Base de datos | PostgreSQL 17 + pgvector, migraciones con Liquibase |
| Configuración | Spring Cloud Consul 2025.1.3 |
| Utilidades | Lombok, MapStruct 1.6.3 |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers |

## Requisitos

- Java 21. Maven no hace falta: el proyecto incluye Maven Wrapper (`./mvnw`).
- PostgreSQL con la extensión pgvector y una base de datos para el servicio (`rag-service-db`).
- Consul con la configuración del servicio (ver abajo).
- Docker, solo para ejecutar los tests.

## Configuración

El servicio lee su configuración y sus secretos de Consul, en las claves de
`config/assistant/`, que comparte con el servicio de consulta. `application.properties` solo
contiene lo que no depende del entorno.

Claves que debe tener Consul:

| Clave | Ejemplo | Para qué |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/rag-service-db` | Base de datos |
| `spring.datasource.username` / `password` | | Credenciales de la base de datos |
| `spring.ai.openai.api-key` | | API key de OpenAI |
| `spring.ai.openai.embedding.options.model` | `text-embedding-3-large` | Modelo de embeddings (3072 dimensiones) |
| `spring.ai.openai.embedding.metadata-mode` | `NONE` | Embeber solo el texto del chunk. Por defecto (`EMBED`) Spring AI antepone la metadata (url, pageId, ids internos) y baja la similitud |
| `spring.ai.vectorstore.pgvector.table-name` | `confluence_chunk` | Tabla de los chunks |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `false` | La tabla la crea Liquibase |
| `spring.ai.vectorstore.pgvector.dimensions` | `3072` | Debe coincidir con la columna `embedding` |
| `spring.ai.vectorstore.pgvector.distance-type` | `COSINE_DISTANCE` | |
| `spring.ai.vectorstore.pgvector.index-type` | `NONE` | pgvector no indexa más de 2000 dimensiones |
| `app.confluence.base-url` | `https://sitio.atlassian.net` | Sin `/wiki` ni `/` final |
| `app.confluence.email` | | Usuario de Confluence |
| `app.confluence.api-token` | | API token de Confluence |

> `table-name` y `dimensions` deben coincidir con el changeset `002-confluence-chunk.sql`.
> Cambiarlos en Consul sin cambiar la tabla hace que falle el guardado de chunks.

Opcional: `spring.cloud.consul.config.watch.enabled=false` evita los `ReadTimeoutException` que el
watcher de Consul escribe en el log. Los cambios en Consul se aplican entonces al reiniciar.

## Base de datos

Liquibase crea el esquema al arrancar (`src/main/resources/db/changelog`):

| Changeset | Qué crea |
|---|---|
| `001-enable-pgvector.sql` | Extensión `vector` |
| `002-confluence-chunk.sql` | `confluence_chunk`: `id`, `content`, `metadata` (jsonb, índice GIN) y `embedding vector(3072)`. Mismas columnas que la tabla por defecto de Spring AI. |
| `003-confluence-page.sql` | `confluence_page`: `page_id`, `space_key`, `title`, `version`, `chunk_count`, `indexed_at` |

Para crear la base de datos en el contenedor de PostgreSQL:

```bash
docker exec rag-service-db-1 sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE DATABASE \"rag-service-db\""'
```

## Arrancar el servicio

```bash
./mvnw spring-boot:run
```

Escucha en el puerto **8082**. Para comprobar que está conectado a PostgreSQL y a Consul:

```bash
curl -s localhost:8082/actuator/health
```

## Uso

```bash
# Páginas del espacio
curl -s "localhost:8082/pages?spaceKey=DS"
# [{"id":"458864","title":"Desarrollo Software","version":1}, ...]

# Ingesta completa
curl -s -X POST "localhost:8082/ingestions?spaceKey=DS"
# {"pages":4,"chunks":9}

# Sincronización
curl -s -X POST "localhost:8082/synchronizations?spaceKey=DS"
# {"newPages":1,"updatedPages":0,"unchangedPages":4,"chunks":2}
```

Las operaciones son síncronas: la respuesta llega cuando han terminado.

### Errores

Los errores siguen el formato `ProblemDetail` (RFC 9457, `application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Space 'XX' does not exist in Confluence",
  "instance": "/ingestions"
}
```

| Status | Cuándo | `detail` |
|---|---|---|
| 400 | Falta `spaceKey` | Mensaje de Spring MVC |
| 404 | El espacio no existe en Confluence | `Space '<key>' does not exist in Confluence` |
| 502 | Confluence rechaza las credenciales (401/403) | `Confluence rejected the credentials` |
| 502 | Confluence no responde o devuelve otro error | `Could not retrieve information from Confluence` |
| 502 | Falla OpenAI al generar embeddings | `Could not generate embeddings with OpenAI` |
| 500 | Cualquier otro error | `Internal service error` |

El detalle técnico de los errores 502 y 500 se escribe en el log, nunca en la respuesta.

## Métricas

El servicio expone `/actuator/prometheus` (formato Prometheus, vía Micrometer) y `/actuator/metrics`.
El Prometheus y el Grafana del proyecto (`docker-compose.yml` en `spec-development-driven/rag-service`)
recogen esas métricas cada 15 s; Grafana está en `http://localhost:3000` (usuario `admin`, contraseña
`admin`) con el panel "JVM (Micrometer)" provisionado.

Para consultar un valor a mano:

```bash
curl -s localhost:8082/actuator/metrics/http.server.requests
```

## Tests

```bash
./mvnw test
```

Necesita Docker: los tests de los services arrancan un PostgreSQL con pgvector temporal
(Testcontainers) y usan embeddings falsos, así que no llaman a OpenAI ni a Confluence.

| Test | Tipo | Qué cubre |
|---|---|---|
| `ConfluenceClientTest` | Unitario con servidor HTTP simulado | Paginación, listado sin cuerpo, `getPage`, espacio inexistente |
| `IngestionServiceTest` | Integración (PostgreSQL real) | Metadata de los chunks, registro de páginas, reingesta sin duplicados, un chunk por sección con `Título > Encabezado` |
| `SynchronizationServiceTest` | Integración (PostgreSQL real) | Primera sincronización, páginas nuevas / actualizadas / sin cambios |
| `HtmlSectionSplitterTest` | Unitario | Una sección por encabezado, encabezados dentro de macros, páginas sin encabezados |
| `PageChangeTest` | Unitario | Clasificación NEW / UPDATED / UNCHANGED |
| `IngestionControllerTest`, `PagesControllerTest` | `@WebMvcTest` | Respuestas 200 y todos los errores |

## Estructura del código

```
com.rag.ingestion
├── client/        Cliente de la API de Confluence, sus DTOs y su mapper
├── common/        PageIndexer y modelos compartidos por client y services
├── config/        RestClient de Confluence y propiedades app.confluence.*
├── controller/    Endpoints, DTOs de respuesta, mappers y manejo de errores
├── entity/        Entidad JPA de confluence_page
├── exception/     Excepciones y mensajes de error
├── mapper/        Mapper de la entidad
├── repository/    Repositorio JPA
└── service/       Ingesta, sincronización y listado, con sus modelos y mappers
```

Los comentarios del código siguen [docs/comment-guidelines.md](docs/comment-guidelines.md).

## Limitaciones conocidas

- Ingesta y sincronización son **síncronas**: con muchas páginas la petición puede tardar.
- Si una página falla (Confluence u OpenAI), **se interrumpe toda la operación**. Las páginas ya
  procesadas quedan guardadas; basta con volver a lanzarla.
- Las páginas **borradas en Confluence no se eliminan** de `confluence_chunk` ni de
  `confluence_page`.
- La **primera llamada a Confluence** tras arrancar tarda unos 10 segundos más que las siguientes;
  las siguientes tardan menos de un segundo.
- Los endpoints `GET /pages`, `POST /ingestions` y `POST /synchronizations` son provisionales: no
  tienen versión en la ruta ni contrato OpenAPI.
