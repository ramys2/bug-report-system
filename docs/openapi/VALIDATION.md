# Validating the OpenAPI documentation

Back to the [README](../../README.md#api-documentation). How the spec is generated: [GENERATE.md](GENERATE.md).

This guide takes about 10 minutes. Work through the steps in order, in the same terminal, because the
`curl` steps reuse shell variables. Commands run from the repository root unless a step says otherwise.
The demo accounts and data come from `BugReportApplication`; see the README for the
[demo credentials](../../README.md#demo-credentials).

**Before you start**, the application needs MariaDB, Artemis and Mailpit. Run from the repository root:

```bash
docker compose up -d database mailpit artemis
```

Expected: `docker compose ps` lists `database`, `mailpit` and `artemis` as running.
If it fails: Docker is not running, or ports `3306`, `61616` or `1025` are already taken by another process.

## 1. Build and regenerate the spec

Run from `backend/`:

```bash
mvn verify -Popenapi -DskipTests
```

Expected: `BUILD SUCCESS`. The log contains `spring-boot-maven-plugin:4.1.0:start`,
`springdoc-openapi-maven-plugin:1.5:generate` and `spring-boot-maven-plugin:4.1.0:stop`, in that order.
`docs/openapi/openapi.yaml` is written. Then run from the repository root:

```bash
git status --short docs/openapi/openapi.yaml
```

Expected: no output if the API did not change since the last commit, or ` M docs/openapi/openapi.yaml` if it did.

If it fails:
- `Connection refused` or the application does not start: the infrastructure from "Before you start" is not running.
- `Address already in use`: something uses port `8089`; rerun with `-Dopenapi.port=8090`.
- `No plugin found for prefix` or a download error: Maven cannot reach Maven Central.

## 2. Start the application

Run from `backend/` (the domain module must be installed once, because `bug-report-api` depends on it):

```bash
mvn -q install -DskipTests
```

Expected: no output and no error.

Then start the application, still in `backend/`. The three variables point it at the containers on `localhost`,
because `application.yml` uses the Docker service names (`database`, `artemis`, `mailpit`):

```bash
SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/bug_report SPRING_ARTEMIS_BROKER_URL=tcp://localhost:61616 SPRING_MAIL_HOST=localhost mvn spring-boot:run -pl bug-report-api
```

Expected: after a few seconds the log shows `Started BugReportApplication in ... seconds`, and the first run also logs
`Initialized demo database: 4 users, 1 projects, 2 components, 3 bug reports, 3 comments, ...`.
A `Could not refresh JMS Connection for destination 'bug-report-event'` error that repeats every 5 seconds is
harmless for this guide, but it means Artemis is not reachable yet.
Leave this terminal running and open a **second terminal** for the next steps (start in the repository root).

If it fails: `Unable to find a suitable main class` means `-am` was added to the command; remove it, because `-am`
also runs the goal on the parent POM. `Communications link failure` means MariaDB is not up yet.
`Port 8080 was already in use` means another backend is running; stop it first.

## 3. Open Swagger UI

Open <http://localhost:8080/swagger-ui.html> in a browser.

Expected:
- The page title is "Bug Report System API", version `0.1.0-SNAPSHOT`, and the description explains the session cookie and CSRF flow.
- Six groups are listed: **Authentication** (4 operations), **Bug reports** (15), **Comments** (3), **Projects** (4),
  **Components** (5) and **Accounts** (5). That is 36 in total.
- Under **Schemas** at the bottom, for example `CreateBugReportRequest` shows field descriptions, example values,
  and `title`, `projectId`, `componentId` and `severity` marked as required. `BugReportBriefResponse` shows an example
  `createdAt` of `2026-05-14T09:30:00`.
- Every write operation (POST, PATCH, DELETE) has a required `X-CSRF-TOKEN` header field.

Then try it out, in this order (**Try it out**, then **Execute**):
1. `GET /api/csrf`: expected `200` with `{"headerName":"X-CSRF-TOKEN","parameterName":"_csrf","token":"..."}`. Copy the token.
2. `POST /api/auth/login`: paste the token into `X-CSRF-TOKEN`, set `username` to `admin@bugreport.local` and `password` to `Admin123!`. Expected: `200` with an empty body.
3. `GET /api/csrf` **again** and copy the new token. The token changes when you sign in.
4. `GET /api/reports`: expected `200` and a list of the three demo reports.
5. `POST /api/comments`: paste the new token and use the body `{"reportId": "<id>", "content": "Try it out from Swagger UI"}`, with the `reportId` of "Severity selector overflows on mobile" from step 4. Expected: `201` with `id`, `authorId`, `authorName`, `content` and `createdAt`.

If it fails:
- The page does not load or shows `403`: the docs URLs are not permitted in `SecurityConfig`.
- `403` with `{"message":"Access denied."}` on a POST or login: the token is missing, or it is the old one from before login.
- `401` with `{"message":"Authentication is required."}` on a GET: you are not signed in, or the browser blocks the session cookie.

## 4. Fetch the raw spec

```bash
curl -s http://localhost:8080/v3/api-docs.yaml | head
```

Expected: the first lines are `openapi: 3.1.0`, `info:` and `  description: |`, followed by the start of the API description.
If it fails: empty output or a connection error means the application is not running (step 2); an HTML page or a
`403` means the docs URLs are not permitted in `SecurityConfig`.

## 5. Call the API with curl

Sign in and keep the session. This block sets `COOKIES` and `TOKEN` for the next blocks:

```bash
COOKIES=/tmp/bugreport-cookies.txt; rm -f $COOKIES
TOKEN=$(curl -s -c $COOKIES -b $COOKIES http://localhost:8080/api/csrf | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
curl -s -c $COOKIES -b $COOKIES -o /dev/null -w "%{http_code}\n" -X POST -H "X-CSRF-TOKEN: $TOKEN" -d 'username=admin@bugreport.local&password=Admin123!' http://localhost:8080/api/auth/login
TOKEN=$(curl -s -c $COOKIES -b $COOKIES http://localhost:8080/api/csrf | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

Expected: a single line, `200`. This matches `POST /api/auth/login`, response `200`. The last line fetches a new token,
because the old one is invalid after login.
If it fails: `403` means the token was not accepted (run the block again from the top); `401` means a wrong email or password.

**A GET.** List the reports:

```bash
curl -s -b $COOKIES -w "\n%{http_code}\n" http://localhost:8080/api/reports
```

Expected: status `200` and a JSON array of three objects (more if you created reports), each like
`{"reportId":"...","title":"Severity selector overflows on mobile","author":"Alice Admin","assignee":null,"status":"OPEN","severity":"LOW","createdAt":"2026-09-30T13:42:11.123456"}`.
The other two are "Valid users cannot sign in" (`IN_PROGRESS`, `CRITICAL`) and "Profile page is blank without an avatar" (`CLOSED`, `MEDIUM`).
This matches `GET /api/reports`, response `200`, schema `BugReportBriefResponse`.
If it fails: `401` means the session cookie was not sent (repeat the sign-in block).

**A POST.** Add a comment to the open report (this adds a comment every time you run it):

```bash
OPEN_ID=$(curl -s -b $COOKIES http://localhost:8080/api/reports | grep -o '"reportId":"[^"]*","title":"Severity' | cut -d'"' -f4)
curl -s -b $COOKIES -w "\n%{http_code}\n" -X POST -H "X-CSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' -d "{\"reportId\":\"$OPEN_ID\",\"content\":\"Checked from the validation guide.\"}" http://localhost:8080/api/comments
```

Expected: status `201` and a body like
`{"id":"...","authorId":"...","authorName":"Alice Admin","content":"Checked from the validation guide.","createdAt":"2026-09-30T21:54:27.109210036"}`.
This matches `POST /api/comments`, response `201`, schema `CreateCommentResponse`.
If it fails: `403` means the token is old (run the sign-in block again); `400` means the JSON body is malformed;
an empty `OPEN_ID` (the body contains `"reportId":""`, which answers `400`) means the demo data was changed and no report has that title.

**An error case (409).** Commenting on the closed report:

```bash
CLOSED_ID=$(curl -s -b $COOKIES http://localhost:8080/api/reports | grep -o '"reportId":"[^"]*","title":"Profile' | cut -d'"' -f4)
curl -s -b $COOKIES -w "\n%{http_code}\n" -X POST -H "X-CSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' -d "{\"reportId\":\"$CLOSED_ID\",\"content\":\"Too late.\"}" http://localhost:8080/api/comments
```

Expected: status `409` and the body `{"message":"Comments cannot be changed on a closed report."}`.
This matches the `409` response of `POST /api/comments`, schema `ApiErrorResponse`
(the same `{"message": "..."}` body is used for every error).

**An error case (404).** A report id that does not exist:

```bash
curl -s -b $COOKIES -w "\n%{http_code}\n" http://localhost:8080/api/reports/00000000-0000-0000-0000-000000000000
```

Expected: status `404` and `{"message":"Report with id: 00000000-0000-0000-0000-000000000000"}`.
This matches the `404` response of `GET /api/reports/{reportId}`, schema `ApiErrorResponse`.

## 6. Lint the spec

```bash
npx -y @redocly/cli lint docs/openapi/openapi.yaml
```

Expected: `Woohoo! Your API description is valid.` and `You have 7 warnings.`, with exit code 0.
The linter reads `redocly.yaml` in the repository root, which uses the recommended rules except `security-defined`.
Without that setting, the same command reports 36 errors (one per operation), because the spec declares no
security scheme. That is intentional: the project documents the session-cookie and CSRF flow in the API
description text instead.

The 7 known warnings, which are left as they are:
- `info-license` (1): the spec has no license, because the project has none to reference.
- `no-server-example.com` (1): springdoc adds the server `http://localhost:8080` automatically.
- `operation-4xx-response` (1): `GET /api/csrf` is public and has no error response.
- `no-invalid-schema-examples` (4): the `createdAt` and `updatedAt` examples (for example `2026-05-14T09:30:00`)
  have no time zone offset. The API really does return times without an offset (Java `LocalDateTime`),
  so the examples match the real output and the `date-time` format is what is imprecise.

If it fails: any error, or a warning not listed above, is a regression from a recent annotation change.
`npx` failing to download means no network access (Node.js and npm are required).

## 7. Check for stale docs

Regenerate from `backend/` (the application from step 2 can keep running, because generation uses port `8089`):

```bash
mvn verify -Popenapi -DskipTests
```

Expected: `BUILD SUCCESS`. Then run from the repository root:

```bash
git diff --exit-code docs/openapi/; echo "exit code: $?"
```

Expected: no diff and `exit code: 0` if the committed spec matches the code. This is the same check CI runs
(`.github/workflows/openapi.yml`). If it prints a diff and `exit code: 1`, the spec was stale or you changed
annotations: review the diff and commit the regenerated `docs/openapi/openapi.yaml`.

## 8. Completeness check

```bash
grep -cE '^    (get|post|put|patch|delete):' docs/openapi/openapi.yaml; grep -cE '^  /' docs/openapi/openapi.yaml
```

Expected: two lines, `36` (operations) and `31` (paths).

Compare with the endpoints found in the code: 15 bug report, 3 comment, 4 project, 5 component and 5 account
endpoints, plus `GET /api/csrf` and `GET /api/auth/me`, is **34 controller endpoints**. The spec has 2 more
(`POST /api/auth/login` and `POST /api/auth/logout`), which are handled by Spring Security and added by
`OpenApiConfig`. 34 + 2 = 36.
If the number is lower: an endpoint is missing or hidden from the spec; if it is higher, a new endpoint was added.

## Clean up

Stop the application in the first terminal with `Ctrl+C`. Then run from the repository root:

```bash
docker compose down
```

Expected: the three containers are removed. The database volume is kept, so the demo data, including the
comment added in step 5, stays until you run `docker compose down -v`.
