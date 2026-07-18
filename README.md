# Customer Forms Hub — Backend

Spring Boot backend for the Customer Forms Hub capstone project. It stores customer questionnaire tickets, extracts questions from Excel or Word files, searches approved knowledge, tracks SME requests, and exports final answers.

The frontend repository is [customer-form-hub-frontend](https://github.com/alisonzzzz-y/customer-form-hub-frontend).

## What you need

- Java 21
- MySQL 8+
- An OpenAI API key

Maven does not need to be installed separately. The repository includes the Maven wrapper.

## Run locally

Create a MySQL database:

```sql
CREATE DATABASE formhub;
```

Set the required environment variables:

```bash
export OPENAI_API_KEY="your-key"
export DB_URL="jdbc:mysql://localhost:3306/formhub"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
```

Start the backend:

```bash
./mvnw spring-boot:run
```

The API will be available at:

```text
http://localhost:8080/api
```

Check that it is running:

```bash
curl http://localhost:8080/api/tickets
```

## Environment variables

| Variable | Required | Default | Purpose |
|---|---:|---|---|
| `OPENAI_API_KEY` | Yes | — | Question classification and knowledge search |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/formhub` | MySQL connection URL |
| `DB_USERNAME` | No | `root` | MySQL username |
| `DB_PASSWORD` | No | empty | MySQL password |
| `PORT` | No | `8080` | Server port |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` | Comma-separated frontend origins |
| `OPENAI_BASE_URL` | No | `https://api.openai.com/v1` | OpenAI-compatible API base URL |
| `OPENAI_CONNECT_TIMEOUT` | No | `5s` | OpenAI connection timeout |
| `OPENAI_READ_TIMEOUT` | No | `60s` | OpenAI response timeout |
| `SHOW_SQL` | No | `false` | Print SQL statements while developing |

If the frontend runs on a different port, update `CORS_ALLOWED_ORIGINS` before starting the backend.

## Main workflow

```text
Create ticket
→ upload questionnaire
→ extract and classify questions
→ search approved knowledge
→ route unresolved questions to SMEs
→ approve final answers
→ export the completed questionnaire
```

Supported questionnaire files:

- `.xlsx`
- `.docx`

## Common endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/tickets` | List tickets |
| `POST` | `/api/tickets` | Create a ticket |
| `PATCH` | `/api/tickets/{id}/status` | Update ticket status |
| `POST` | `/api/questionnaire/classify` | Parse and classify a file without saving it |
| `POST` | `/api/questionnaire/import?ticketId={id}` | Parse, classify, and save questions |
| `GET` | `/api/questions/ticket/{ticketId}` | List questions for a ticket |
| `POST` | `/api/knowledge-base/search` | Search approved knowledge |
| `POST` | `/api/final-answers` | Create or update a final answer |
| `GET` | `/api/final-review/ticket/{ticketId}` | Load the final review data |
| `GET` | `/api/export/ticket/{ticketId}` | Download the completed Excel file |

Ticket statuses:

```text
New · AI Processing · Intake Review · In Progress · Waiting SME
Ready for Review · Approved · Sent · Closed · Archived
```

Knowledge statuses:

```text
Draft · Pending Review · Approved · Deprecated · Archived
```

Only `Approved` knowledge entries are used for answer suggestions.

## Demo data

When the database tables are empty, `DataLoader` adds sample tickets, questions, SME requests, and knowledge entries. It also generates embeddings for knowledge entries that do not have one yet.

This is useful for the demo, but keep it in mind when starting the app with a new database.

## Tests

Run the full test suite:

```bash
./mvnw clean test
```

Build the deployable JAR:

```bash
./mvnw clean package
```

The JAR is written to `target/`.

## Docker

Build and run the image:

```bash
docker build -t customer-form-hub .
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY="your-key" \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/formhub" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="your-password" \
  customer-form-hub
```

## Project structure

```text
controller/   HTTP endpoints
service/      workflow and business logic
repository/   database access
entity/       JPA database models
dto/          API response and request objects
config/       CORS, OpenAI client, error handling, and demo data
```
