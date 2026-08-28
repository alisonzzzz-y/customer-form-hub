# AI Performance V1 - Repository Implementation Plan

AI Performance is implemented across the Spring Boot backend and React frontend. V1 reports the latest review outcome for each AI-assisted question and the latest completed offline retrieval benchmark.

## Confirmed review mapping

- Approve an unchanged knowledge suggestion -> `ACCEPTED`
- Edit or rewrite a knowledge suggestion, then approve -> `EDITED`
- Route an AI-assisted question to an SME -> `ESCALATED`
- Request AE clarification for an AI-assisted question -> `ESCALATED`
- A manual answer created without a knowledge suggestion -> excluded from AI review metrics
- Unapprove or reopen -> clear the current outcome until another terminal decision is made

V1 does not add a Reject action, immutable review-event history, Flyway, or a new authentication system. The UI exposes the page to the existing Manager demo role, but this is not represented as backend authorization.

## Reused architecture and minimal data changes

The backend reuses `FormQuestion`, `FinalAnswer`, `SmeRequest`, `FinalAnswerService`, and `RetrievalService`. `form_question` gains the AI source ID, latest outcome, decision time, and AE clarification request time. `knowledge_base` gains a stable source key. One evaluation-run table stores run-level results, while synthetic benchmark cases remain in a versioned JSON resource.

The frontend reuses `TicketWorkflow.tsx`, `services/backend.ts`, existing cards, responsive layout, loading/error patterns, and demo roles. `Waiting AE` ends the current question review but blocks final ticket review. SME packaging may continue for other questions.

SME `sentAt` and AE request timestamps represent the in-product request or draft action, not verified delivery by an external mail client.

## API and operation

- `POST /api/questions/{id}/review-escalation`
- `POST /api/questions/{id}/review-reopen`
- `GET /api/ai-performance/review-summary`
- `GET /api/ai-performance/retrieval-runs`
- `GET /api/ai-performance/retrieval-runs/{runId}`

The default review period is the last 30 days in UTC. Empty review periods return null rates. The benchmark is run through the opt-in `AI_EVALUATION_RUN=true` application setting, not a public dashboard trigger.

## Latest-outcome limitation

V1 keeps operational timestamps but not a complete immutable decision history. A later decision replaces the outcome and decision time used by analytics. Historical as-of reporting is reserved for a future audit enhancement.

## Verification

- Backend tests cover accepted, edited, escalated, reopened, no-data, date filtering, Top-1/Top-3 scoring, short result lists, and retrieval failures.
- Frontend unit, type, build, visual, and E2E checks cover the Manager page, Ask AE, reopen, SME progression, and final-review blocking.
- The review reconciliation is `reviewed = accepted + edited + escalated`; rejected remains zero in V1.
