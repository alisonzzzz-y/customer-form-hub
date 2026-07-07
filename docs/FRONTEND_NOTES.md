

# Frontend Integration Notes

This file is a practical guide for frontend development.

For full endpoint details, request bodies, and response examples, see `API.md`.

## Backend Base URL

Local development:

```text
http://localhost:8080
```

Recommended frontend setup:

```js
const API_BASE_URL = "http://localhost:8080";
```

Do not hardcode repeated full URLs inside components. Put API calls in a small API layer, for example:

```text
src/api/ticketApi.js
src/api/questionApi.js
src/api/finalAnswerApi.js
src/api/smeRequestApi.js
```

---

# 1. Main Frontend Flow

The product flow is:

```text
Ticket Dashboard
→ Ticket Detail
→ Upload Questionnaire
→ Review Imported Questions
→ Knowledge Base Suggestions / SME Routing
→ Final Review
→ Export Answers
```

Suggested pages:

```text
TicketListPage
TicketDetailPage
QuestionnaireUploadPage
QuestionReviewPage
SmeRequestsPage
FinalReviewPage
```

The frontend should usually start from a selected `ticketId`.

---

# 2. Important Status Values

Use the exact backend strings. Do not invent different casing or underscore versions.

## Question status

```text
Needs Review
Source Found
SME Needed
Answered
```

Frontend meaning:

```text
Needs Review
= analyst still needs to check the question

Source Found
= knowledge base suggestion exists

SME Needed
= analyst needs expert input from a department

Answered
= final answer has been confirmed
```

## Final answer approval status

```text
Draft
Confirmed
```

Frontend meaning:

```text
Draft
= saved but not customer-ready

Confirmed
= reviewed and approved by analyst
```

Important rule:

```text
Draft answers must not be treated as final customer-facing answers.
```

## SME request status

```text
Waiting for ETA
ETA Confirmed
Overdue
Returned
```

## SME request question status

```text
Pending
Returned
```

## Departments

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

# 3. Upload Flow

Supported files:

```text
.xlsx
.docx
```

Use this endpoint to upload and save questions to a ticket:

```http
POST /api/questionnaire/import?ticketId={ticketId}
```

Request type:

```text
multipart/form-data
```

Frontend example:

```js
export async function importQuestionnaire(ticketId, file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/api/questionnaire/import?ticketId=${ticketId}`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error("Failed to import questionnaire");
  }

  return response.json();
}
```

Notes:

```text
Do not manually set Content-Type for FormData.
The browser will set the multipart boundary automatically.
```

After upload succeeds, reload questions:

```http
GET /api/questions/ticket/{ticketId}
```

---

# 4. Questions Flow

Get all questions for one ticket:

```http
GET /api/questions/ticket/{ticketId}
```

Update question status:

```http
PATCH /api/questions/{id}/status
```

Request body:

```json
{
  "status": "SME Needed"
}
```

Suggested frontend behavior:

```text
User clicks "Send to SME"
→ frontend PATCHes question status to "SME Needed"
→ frontend refreshes question list
```

---

# 5. Knowledge Base Suggestions Flow

Use this endpoint when the analyst opens a question and wants recommended answer sources:

```http
GET /api/questions/{id}/suggestions
```

Important:

```text
Send the question id only.
Do not send question text back to the backend.
```

The backend loads the question, embeds it, searches the knowledge base, and returns top matches.

Possible response:

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

Frontend display suggestions:

```text
Show content preview
Show source / documentTitle / sectionTitle
Show similarity score if useful
Let analyst choose a chunk as the source for final answer
```

Empty array means:

```text
No strong knowledge base match was found.
The analyst may need to mark the question as SME Needed.
```

---

# 6. Final Answer Flow

Save or update a final answer:

```http
POST /api/final-answers
```

Important:

```text
This endpoint is an upsert by questionId.
If an answer already exists for the question, the backend updates it instead of creating a duplicate.
```

Save draft example:

```json
{
  "questionId": 10,
  "sourceChunkId": 21,
  "answerText": "Draft answer text",
  "isEdited": true,
  "sourceType": "Knowledge Base",
  "approvalStatus": "Draft",
  "approvedBy": null
}
```

Confirmed answer example:

```json
{
  "questionId": 10,
  "sourceChunkId": 21,
  "answerText": "Confirmed answer text",
  "isEdited": true,
  "sourceType": "Knowledge Base",
  "approvalStatus": "Confirmed",
  "approvedBy": "Alison"
}
```

Important backend side effect:

```text
If approvalStatus is "Confirmed", the backend automatically sets the linked question status to "Answered".
```

Frontend rule:

```text
After saving a Confirmed answer, reload the question or final review data so the UI shows status = Answered.
```

---

# 7. SME Dispatch Flow

Analyst marks some questions as:

```text
SME Needed
```

Then frontend calls:

```http
POST /api/sme-requests/dispatch/ticket/{ticketId}
```

Backend behavior:

```text
Finds all SME Needed questions in the ticket
Groups them by department
Creates one SME request per department if missing
Reuses existing requests if already created
Links the matching questions into each SME request
```

Important:

```text
This endpoint is idempotent.
Calling it twice should not create duplicate SME requests or duplicate question links.
```

Frontend button example:

```text
Button: Dispatch SME Requests
On click: POST /api/sme-requests/dispatch/ticket/{ticketId}
Then reload: GET /api/sme-requests/ticket/{ticketId}
```

---

# 8. SME Email Preview Flow

Get rendered email content:

```http
GET /api/sme-requests/{id}/email
```

Response:

```json
{
  "to": "",
  "subject": "[Action needed] SME input for Globex Inc questionnaire – InfoSec (1 questions)",
  "body": "Hi InfoSec Team,\n\nThe GOM team is completing a customer questionnaire for Globex Inc..."
}
```

Frontend notes:

```text
to is intentionally empty.
The analyst chooses the recipient in their mail client.
subject and body are plain text.
```

Mailto example:

```js
export function buildMailto(email) {
  const subject = encodeURIComponent(email.subject);
  const body = encodeURIComponent(email.body);
  return `mailto:${email.to || ""}?subject=${subject}&body=${body}`;
}
```

Suggested UI:

```text
Show subject preview
Show body preview
Button: Open Email Client
```

---

# 9. Returned SME Answer Flow

Get questions linked to one SME request:

```http
GET /api/sme-request-questions/request/{smeRequestId}
```

Record returned answer:

```http
PATCH /api/sme-request-questions/{id}/answer
```

Request body:

```json
{
  "returnedAnswer": "We review privileged access quarterly and enforce MFA."
}
```

Frontend behavior:

```text
SME answer is stored on the SME request question link.
Analyst can later copy/edit it into a FinalAnswer if it is customer-ready.
```

---

# 10. Final Review Flow

Get final review rows:

```http
GET /api/final-review/ticket/{ticketId}
```

Each row combines question data and answer data.

Important fields:

```text
questionId
questionText
department
questionStatus
answerId
answerText
approvalStatus
sourceChunkId
answerUpdatedAt
answered
```

Frontend meaning:

```text
answered = final answer row exists
approvalStatus = Draft or Confirmed
answerUpdatedAt = freshness timestamp
sourceChunkId = traceability to knowledge base chunk
```

Suggested UI:

```text
Show all questions in one table
Highlight Draft answers
Highlight unanswered questions
Show Last Updated timestamp
Provide Confirm / Edit actions
Provide Export button
```

---

# 11. Export Flow

Download Excel export:

```http
GET /api/export/ticket/{ticketId}
```

Frontend example:

```js
export async function downloadExport(ticketId) {
  const response = await fetch(`${API_BASE_URL}/api/export/ticket/${ticketId}`);

  if (!response.ok) {
    throw new Error("Failed to export answers");
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `ticket-${ticketId}-answers.xlsx`;
  link.click();
  window.URL.revokeObjectURL(url);
}
```

Export rules:

```text
Confirmed answers are exported in full.
Draft answers are not exported as customer-facing text.
Draft answer cells show: (Draft – not confirmed)
Questions with no answer show: (No answer yet)
```

---

# 12. Error Handling

Recommended frontend helper:

```js
export async function requestJson(url, options = {}) {
  const response = await fetch(url, options);

  if (!response.ok) {
    let message = "Request failed";
    try {
      const error = await response.json();
      message = error.message || message;
    } catch {
      message = `${response.status} ${response.statusText}`;
    }
    throw new Error(message);
  }

  return response.json();
}
```

Common statuses:

```text
400 = bad request, usually missing or invalid input
404 = entity not found
405 = wrong HTTP method
500 = backend error
```

---

# 13. Recommended API Layer

Keep API calls out of page components when possible.

Suggested files:

```text
src/api/ticketApi.js
src/api/questionnaireApi.js
src/api/questionApi.js
src/api/knowledgeBaseApi.js
src/api/finalAnswerApi.js
src/api/smeRequestApi.js
src/api/exportApi.js
```

Example:

```js
import { API_BASE_URL } from "./config";

export async function getQuestionsByTicket(ticketId) {
  const response = await fetch(`${API_BASE_URL}/api/questions/ticket/${ticketId}`);
  if (!response.ok) {
    throw new Error("Failed to load questions");
  }
  return response.json();
}
```

Page components should focus on:

```text
state
loading
error
rendering
user actions
```

API files should focus on:

```text
fetch URL
HTTP method
request body
response parsing
```

---

# 14. Things Frontend Should Not Do

```text
Do not generate embeddings in the frontend.
Do not send OpenAI API keys to the frontend.
Do not manually create knowledge base embeddings.
Do not treat Draft answers as final.
Do not use random status strings.
Do not assume SME dispatch creates a new request every time.
Do not manually set multipart Content-Type for file uploads.
Do not rely on question text as an identifier; use questionId.
```

---

# 15. Quick Workflow Checklist

## Basic demo path

```text
1. GET /api/tickets
2. Open one ticket
3. POST /api/questionnaire/import?ticketId={ticketId}
4. GET /api/questions/ticket/{ticketId}
5. GET /api/questions/{questionId}/suggestions
6. POST /api/final-answers with approvalStatus = Confirmed
7. GET /api/final-review/ticket/{ticketId}
8. GET /api/export/ticket/{ticketId}
```

## SME demo path

```text
1. PATCH /api/questions/{id}/status → SME Needed
2. POST /api/sme-requests/dispatch/ticket/{ticketId}
3. GET /api/sme-requests/ticket/{ticketId}
4. GET /api/sme-requests/{smeRequestId}/email
5. Open mail client using mailto link
6. PATCH /api/sme-request-questions/{id}/answer
```