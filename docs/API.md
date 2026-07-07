

# Customer Form Hub API Documentation

Backend API reference for the questionnaire workflow tool.

This file is the frontend/backend contract. If a backend endpoint changes, update this file in the same commit.

## Base URL

```text
http://localhost:8080
```

All routes below are relative to this base URL.

## Common Notes

- Most request and response bodies are JSON.
- File upload endpoints use `multipart/form-data`.
- Date/time fields are ISO strings, for example `2026-07-05T23:07:05.440928`.
- Missing resources usually return `404 Not Found`.
- Invalid input usually returns `400 Bad Request`.

## Shared Status Values

### Ticket status

```text
New
Intake Missing
In Review
Waiting SME
Completed
```

### Question status

```text
Needs Review
Source Found
SME Needed
Answered
```

### SME request status

```text
Waiting for ETA
ETA Confirmed
Overdue
Returned
```

### SME request question status

```text
Pending
Returned
```

### Final answer approval status

```text
Draft
Confirmed
```

### Departments

```text
InfoSec
Legal
HR
Finance
Compliance
ESG
General
```

---

# 1. Tickets

A ticket represents one customer questionnaire workflow.

## Get all tickets

```http
GET /api/tickets
```

### Response 200

```json
[
  {
    "id": 2,
    "customerName": "Globex Inc",
    "createdBy": "Jane Smith",
    "assignedTo": "Sarah",
    "status": "Intake Missing",
    "urgency": "High",
    "ndaStatus": "Unknown",
    "deadline": "2025-05-26T00:00:00",
    "businessImpact": "Renewal, medium value",
    "eta": "2025-05-23T15:00:00",
    "createdAt": "2025-05-19T09:07:00"
  }
]
```

## Get one ticket

```http
GET /api/tickets/{id}
```

### Response 200

```json
{
  "id": 2,
  "customerName": "Globex Inc",
  "createdBy": "Jane Smith",
  "assignedTo": "Sarah",
  "status": "Intake Missing",
  "urgency": "High",
  "ndaStatus": "Unknown",
  "deadline": "2025-05-26T00:00:00",
  "businessImpact": "Renewal, medium value",
  "eta": "2025-05-23T15:00:00",
  "createdAt": "2025-05-19T09:07:00"
}
```

### Response 404

Ticket not found.

## Create a ticket

```http
POST /api/tickets
```

### Request Body

```json
{
  "customerName": "Acme Corp",
  "createdBy": "Jane Smith",
  "assignedTo": "Sarah",
  "status": "New",
  "urgency": "High",
  "ndaStatus": "Unknown",
  "deadline": "2026-08-01T00:00:00",
  "businessImpact": "Strategic renewal",
  "eta": "2026-07-25T17:00:00"
}
```

### Response 200

Returns the created ticket.

## Update a ticket

```http
PUT /api/tickets/{id}
```

### Request Body

Same shape as the ticket object.

### Response 200

Returns the updated ticket.

### Response 404

Ticket not found.

## Update ticket status only

```http
PATCH /api/tickets/{id}/status
```

### Request Body

```json
{
  "status": "Completed"
}
```

### Response 200

Returns the updated ticket.

### Response 400

`status` is missing or blank.

## Delete a ticket

```http
DELETE /api/tickets/{id}
```

### Response 204

No response body.

---

# 2. Questionnaire Upload

Supported upload file types:

```text
.xlsx
.docx
```

`.xlsx` files use the structured Excel parser. `.docx` files are converted to raw text, then the LLM extracts questions.

## Parse questionnaire only

```http
POST /api/questionnaire/parse
Content-Type: multipart/form-data
```

### Form Data

| Name | Type | Required | Description |
|---|---|---|---|
| file | File | Yes | `.xlsx` or `.docx` questionnaire |

### Response 200

```json
[
  {
    "section": "Access Control",
    "questionText": "Do you have a SOC 2 Type II report?"
  }
]
```

## Parse and classify questionnaire

```http
POST /api/questionnaire/classify
Content-Type: multipart/form-data
```

### Form Data

| Name | Type | Required | Description |
|---|---|---|---|
| file | File | Yes | `.xlsx` or `.docx` questionnaire |

### Response 200

```json
[
  {
    "section": "Access Control",
    "questionText": "Do you have a SOC 2 Type II report?",
    "department": "InfoSec"
  }
]
```

## Import questionnaire into a ticket

```http
POST /api/questionnaire/import?ticketId={ticketId}
Content-Type: multipart/form-data
```

### Query Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| ticketId | Long | Yes | Ticket id to attach questions to |

### Form Data

| Name | Type | Required | Description |
|---|---|---|---|
| file | File | Yes | `.xlsx` or `.docx` questionnaire |

### Response 200

Returns saved questions.

```json
[
  {
    "id": 1,
    "ticketId": 2,
    "questionText": "Do you have a SOC 2 Type II report?",
    "department": "InfoSec",
    "status": "Needs Review",
    "riskLevel": null,
    "rowReference": "Access Control",
    "createdAt": "2026-07-03T11:21:05.949594"
  }
]
```

### Response 400

Unsupported file type.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported file type: please upload .xlsx or .docx"
}
```

---

# 3. Questions

A `FormQuestion` represents one question from a customer questionnaire.

## Get all questions

```http
GET /api/questions
```

## Get questions by ticket

```http
GET /api/questions/ticket/{ticketId}
```

### Response 200

```json
[
  {
    "id": 1,
    "ticketId": 2,
    "questionText": "Do you have a SOC 2 Type II report?",
    "department": "InfoSec",
    "status": "Source Found",
    "riskLevel": "Medium",
    "rowReference": "Q1",
    "createdAt": "2026-07-03T11:21:05.949594"
  }
]
```

## Get questions by ticket and department

```http
GET /api/questions/ticket/{ticketId}/department/{department}
```

### Example

```http
GET /api/questions/ticket/2/department/InfoSec
```

## Get one question

```http
GET /api/questions/{id}
```

### Response 404

Question not found.

## Get knowledge base suggestions for one question

```http
GET /api/questions/{id}/suggestions
```

The frontend only sends the question id. The backend loads the question text and runs semantic search.

### Response 200

```json
[
  {
    "id": 21,
    "documentTitle": "Security Standard",
    "sectionTitle": "Access Control",
    "content": "Privileged accounts are reviewed quarterly...",
    "source": "Internal KB",
    "lastUpdated": "2026-07-01T10:00:00",
    "sharingStatus": "External OK",
    "department": "InfoSec",
    "approved": true,
    "similarityScore": 0.82
  }
]
```

### Notes

- Returns top matching approved knowledge base chunks.
- Empty array means no good match was found.
- `similarityScore` is a cosine similarity score from 0 to 1.

## Create a question

```http
POST /api/questions
```

### Request Body

```json
{
  "ticketId": 2,
  "questionText": "Describe data encryption in transit.",
  "department": "InfoSec",
  "status": "Needs Review",
  "riskLevel": "Medium",
  "rowReference": "Q2"
}
```

## Update a question

```http
PUT /api/questions/{id}
```

### Request Body

Same shape as `FormQuestion`.

## Update question status only

```http
PATCH /api/questions/{id}/status
```

### Request Body

```json
{
  "status": "SME Needed"
}
```

## Delete a question

```http
DELETE /api/questions/{id}
```

---

# 4. Knowledge Base

Knowledge base chunks are used for semantic retrieval.

## Get all knowledge base chunks

```http
GET /api/knowledge-base
```

## Get one knowledge base chunk

```http
GET /api/knowledge-base/{id}
```

## Create a knowledge base chunk

```http
POST /api/knowledge-base
```

### Request Body

```json
{
  "documentTitle": "Security Standard",
  "sectionTitle": "Access Control",
  "content": "Privileged accounts are reviewed quarterly.",
  "source": "Internal KB",
  "lastUpdated": "2026-07-01T10:00:00",
  "sharingStatus": "External OK",
  "department": "InfoSec",
  "approved": true
}
```

### Frontend Notes

- Do not send `embedding` from the frontend.
- The backend generates embeddings automatically.
- Only approved chunks should be used for customer-facing answer suggestions.

## Semantic search

```http
POST /api/knowledge-base/search
```

### Request Body

```json
{
  "question": "Do you support encryption in transit?"
}
```

### Response 200

```json
[
  {
    "id": 21,
    "documentTitle": "Security Standard",
    "sectionTitle": "Encryption",
    "content": "All customer data is encrypted in transit using TLS 1.2 or above.",
    "source": "Internal KB",
    "lastUpdated": "2026-07-01T10:00:00",
    "sharingStatus": "External OK",
    "department": "InfoSec",
    "approved": true,
    "similarityScore": 0.89
  }
]
```

## Update a knowledge base chunk

```http
PUT /api/knowledge-base/{id}
```

## Delete a knowledge base chunk

```http
DELETE /api/knowledge-base/{id}
```

---

# 5. Final Answers

`FinalAnswer` stores the analyst-reviewed answer for a question.

Important behavior:

- `POST /api/final-answers` performs an upsert by `questionId`.
- If an answer already exists, non-null request fields update the existing row.
- If `approvalStatus` is `Confirmed`, the linked `FormQuestion.status` is automatically synced to `Answered`.

## Get all final answers

```http
GET /api/final-answers
```

## Get final answer by question id

```http
GET /api/final-answers/question/{questionId}
```

### Response 200

```json
{
  "id": 2,
  "questionId": 10,
  "sourceChunkId": 21,
  "answerText": "Privileged accounts are managed through role-based access controls.",
  "isEdited": true,
  "sourceType": "Knowledge Base",
  "approvalStatus": "Confirmed",
  "approvedBy": "Alison",
  "createdAt": "2026-07-05T23:07:05.440928",
  "updatedAt": "2026-07-05T23:07:18.403393"
}
```

## Get one final answer

```http
GET /api/final-answers/{id}
```

## Save final answer

```http
POST /api/final-answers
```

### Request Body

```json
{
  "questionId": 10,
  "sourceChunkId": 21,
  "answerText": "Privileged accounts are managed through role-based access controls.",
  "isEdited": true,
  "sourceType": "Knowledge Base",
  "approvalStatus": "Confirmed",
  "approvedBy": "Alison"
}
```

### Frontend Notes

Use `approvalStatus: "Draft"` for unconfirmed work.

Use `approvalStatus: "Confirmed"` after analyst review.

## Delete a final answer

```http
DELETE /api/final-answers/{id}
```

---

# 6. Final Review

Final review combines each question with its final answer, if one exists.

## Get final review rows for a ticket

```http
GET /api/final-review/ticket/{ticketId}
```

### Response 200

```json
[
  {
    "questionId": 10,
    "questionText": "How are privileged accounts managed?",
    "department": "InfoSec",
    "questionStatus": "Answered",
    "riskLevel": "Medium",
    "answerId": 2,
    "answerText": "Privileged accounts are managed through role-based access controls.",
    "isEdited": true,
    "sourceType": "Knowledge Base",
    "approvedBy": "Alison",
    "approvalStatus": "Confirmed",
    "sourceChunkId": 21,
    "answerUpdatedAt": "2026-07-05T23:07:18.403393",
    "answered": true
  }
]
```

### Frontend Notes

- `answered` means a final answer row exists.
- `approvalStatus` tells whether the answer is `Draft` or `Confirmed`.
- `sourceChunkId` supports traceability.
- `answerUpdatedAt` is the answer freshness timestamp.

---

# 7. Export

## Export ticket answers as Excel

```http
GET /api/export/ticket/{ticketId}
```

### Response 200

Downloads an `.xlsx` file.

### Response Headers

```text
Content-Disposition: attachment; filename="ticket-{ticketId}-answers.xlsx"
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

### Export Rules

- Confirmed answers are exported in full.
- Draft answers are not exported as customer-facing text.
- Draft answer cells show `(Draft – not confirmed)`.
- Questions with no answer show `(No answer yet)`.
- The export includes a `Last Updated (UTC)` column.

---

# 8. SME Requests

`SmeRequest` represents a department-level request for expert input.

## Get all SME requests

```http
GET /api/sme-requests
```

## Get SME requests by ticket

```http
GET /api/sme-requests/ticket/{ticketId}
```

### Response 200

```json
[
  {
    "id": 1,
    "ticketId": 2,
    "department": "InfoSec",
    "teamName": "InfoSec Team",
    "questionCount": 1,
    "eta": null,
    "status": "Waiting for ETA",
    "confirmedBy": null,
    "sentAt": "2026-07-05T23:00:00",
    "returnedAt": null
  }
]
```

### Notes

Overdue detection is applied when SME requests are read. If ETA has passed and the request has not been returned, status may show as `Overdue`.

## Get one SME request

```http
GET /api/sme-requests/{id}
```

## Render SME request email content

```http
GET /api/sme-requests/{id}/email
```

This endpoint renders plain-text email content. The frontend can use it to build a `mailto:` link.

### Response 200

```json
{
  "to": "",
  "subject": "[Action needed] SME input for Globex Inc questionnaire – InfoSec (1 questions)",
  "body": "Hi InfoSec Team,\n\nThe GOM team is completing a customer questionnaire for Globex Inc and needs your department's input on the questions below.\n\nCustomer deadline: 2025-05-26 (UTC)\n\n1. How are privileged accounts managed?\n\nPlease reply with a realistic ETA for returning answers so we can track this request.\n\nThanks,\nGlobal Order Management"
}
```

### Frontend Notes

- `to` is intentionally empty. The analyst chooses the recipient in their mail client.
- Use `subject` and `body` to build a `mailto:` link.
- Body is plain text, not HTML.

## Create an SME request manually

```http
POST /api/sme-requests
```

### Request Body

```json
{
  "ticketId": 2,
  "department": "InfoSec",
  "teamName": "InfoSec Team",
  "questionCount": 3,
  "eta": null,
  "status": "Waiting for ETA",
  "confirmedBy": null,
  "returnedAt": null
}
```

## Dispatch SME requests for a ticket

```http
POST /api/sme-requests/dispatch/ticket/{ticketId}
```

This is the one-click dispatch endpoint.

### Behavior

- Finds all questions in the ticket with `status = "SME Needed"`.
- Groups them by department.
- Creates one SME request per department if it does not already exist.
- Reuses existing SME requests for the same ticket and department.
- Packages the matching questions into the request.
- Safe to call more than once. It should not create duplicates.

### Response 200

Returns the SME requests involved in the dispatch.

```json
[
  {
    "id": 1,
    "ticketId": 2,
    "department": "InfoSec",
    "teamName": "InfoSec Team",
    "questionCount": 2,
    "eta": null,
    "status": "Waiting for ETA",
    "confirmedBy": null,
    "sentAt": "2026-07-05T23:00:00",
    "returnedAt": null
  }
]
```

If there are no SME-needed questions, returns `200 OK` with an empty array.

```json
[]
```

## Update an SME request

```http
PUT /api/sme-requests/{id}
```

### Request Body

Only non-null fields are copied onto the existing SME request.

```json
{
  "eta": "2026-07-10T17:00:00",
  "status": "ETA Confirmed",
  "confirmedBy": "InfoSec Lead"
}
```

### Response 200

Returns the updated SME request.

## Delete an SME request

```http
DELETE /api/sme-requests/{id}
```

---

# 9. SME Request Questions

`SmeRequestQuestion` links one `FormQuestion` to one `SmeRequest`.

## Get questions linked to an SME request

```http
GET /api/sme-request-questions/request/{smeRequestId}
```

### Response 200

```json
[
  {
    "id": 1,
    "smeRequestId": 1,
    "questionId": 10,
    "status": "Pending",
    "includedReason": "No source found",
    "returnedAnswer": null,
    "updatedAt": "2026-07-05T23:00:00"
  }
]
```

## Get SME request records by question

```http
GET /api/sme-request-questions/question/{questionId}
```

## Get one SME request question link

```http
GET /api/sme-request-questions/{id}
```

## Create one SME request question link manually

```http
POST /api/sme-request-questions
```

### Request Body

```json
{
  "smeRequestId": 1,
  "questionId": 10,
  "status": "Pending",
  "includedReason": "No source found",
  "returnedAnswer": null
}
```

## Package SME-needed questions into a request

```http
POST /api/sme-request-questions/package
```

### Request Body

```json
{
  "smeRequestId": 1,
  "ticketId": 2,
  "department": "InfoSec"
}
```

### Behavior

- Finds all questions for the given ticket and department with `status = "SME Needed"`.
- Links them to the given SME request.
- Skips questions that are already linked.
- Safe to call more than once.

## Record returned SME answer

```http
PATCH /api/sme-request-questions/{id}/answer
```

### Request Body

```json
{
  "returnedAnswer": "We review privileged access quarterly and enforce MFA."
}
```

### Response 200

Returns the updated SME request question link.

### Response 400

`returnedAnswer` is missing or blank.

## Delete one SME request question link

```http
DELETE /api/sme-request-questions/{id}
```

---

# 10. Recommended Frontend Workflow

## Intake and upload

```text
1. User creates or opens a ticket.
2. User uploads .xlsx or .docx questionnaire.
3. Frontend calls POST /api/questionnaire/import?ticketId={ticketId}.
4. Backend parses, classifies, and saves FormQuestion rows.
5. Frontend displays questions from GET /api/questions/ticket/{ticketId}.
```

## Knowledge base answer workflow

```text
1. Frontend opens a question.
2. Frontend calls GET /api/questions/{id}/suggestions.
3. Analyst chooses or edits a suggested answer.
4. Frontend saves to POST /api/final-answers.
5. If approvalStatus is Confirmed, backend marks the question as Answered.
```

## SME workflow

```text
1. Analyst marks uncertain questions as SME Needed.
2. Frontend calls POST /api/sme-requests/dispatch/ticket/{ticketId}.
3. Backend creates/reuses department-level SME requests.
4. Frontend displays requests from GET /api/sme-requests/ticket/{ticketId}.
5. Frontend gets email preview from GET /api/sme-requests/{id}/email.
6. Analyst sends email through their mail client using the rendered subject/body.
7. Returned SME answers are recorded through PATCH /api/sme-request-questions/{id}/answer.
```

## Final review and export

```text
1. Frontend calls GET /api/final-review/ticket/{ticketId}.
2. Analyst reviews all answers.
3. Draft answers can be saved but are not exported as final text.
4. Confirmed answers are exported through GET /api/export/ticket/{ticketId}.
```

---

# 11. Quick cURL Examples

## List tickets

```bash
curl -s http://localhost:8080/api/tickets
```

## Upload and import questionnaire

```bash
curl -s -X POST "http://localhost:8080/api/questionnaire/import?ticketId=2" \
  -F "file=@/path/to/questionnaire.xlsx"
```

## Get suggestions for a question

```bash
curl -s http://localhost:8080/api/questions/1/suggestions
```

## Save a confirmed final answer

```bash
curl -s -X POST http://localhost:8080/api/final-answers \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": 10,
    "answerText": "Test answer",
    "approvalStatus": "Confirmed",
    "approvedBy": "Alison",
    "sourceType": "Knowledge Base"
  }'
```

## Dispatch SME requests for a ticket

```bash
curl -s -X POST http://localhost:8080/api/sme-requests/dispatch/ticket/2
```

## Get SME email preview

```bash
curl -s http://localhost:8080/api/sme-requests/1/email
```

## Download Excel export

```bash
curl -s -o /tmp/ticket-2-answers.xlsx http://localhost:8080/api/export/ticket/2
```