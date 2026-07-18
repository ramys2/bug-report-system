# Bug Report System

The Spring Boot API uses an H2 in-memory database for local development. On startup,
`BugReportApplication` creates a connected demonstration data set containing users,
a software project, components, bug reports, comments, attachments, and a resolution.

Because the database is in memory, the data is recreated whenever the application is
restarted.

## Demo credentials

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@bugreport.local` | `Admin123!` |
| Backend developer | `developer@bugreport.local` | `Developer123!` |
| Frontend developer | `frontend@bugreport.local` | `Frontend123!` |
| Reporter | `reporter@bugreport.local` | `Reporter123!` |

These accounts and passwords are development fixtures only. Authentication has not yet
been implemented, and the current domain model stores these demonstration passwords in
the `passwordHash` field without hashing. Do not reuse them or this storage approach in
production.

## Run locally

From the `backend` directory:

```shell
mvn spring-boot:run -pl bug-report-api -am
```
