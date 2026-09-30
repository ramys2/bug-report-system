# Generating the OpenAPI spec

Back to the [README](../../README.md#api-documentation). To check the result, see [VALIDATION.md](VALIDATION.md).

The spec `docs/openapi/openapi.yaml` is generated from the annotations in the backend code
(`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, ...). The annotations are the source of truth:
never edit the YAML by hand, because the next generation overwrites it.

## How it works

The Maven profile `openapi` in `backend/bug-report-api/pom.xml` runs in the `integration-test` phase:

1. `spring-boot-maven-plugin:start` starts the application on port `8089`.
2. `springdoc-openapi-maven-plugin:generate` downloads `http://localhost:8089/v3/api-docs.yaml`
   and writes it to `docs/openapi/openapi.yaml`.
3. `spring-boot-maven-plugin:stop` stops the application.

It is a profile, not part of the normal build, because starting the application needs MariaDB, Artemis
and Mailpit. A plain `mvn package` or `mvn test` stays as fast as before and does not touch the spec.

## Regenerate locally

The application needs its infrastructure. Run from the repository root:

```bash
docker compose up -d database mailpit artemis
```

Expected result: the three containers are running (`docker compose ps`).

Then generate the spec. Run from `backend/`:

```bash
mvn verify -Popenapi -DskipTests
```

Expected result: `BUILD SUCCESS`, and `docs/openapi/openapi.yaml` is written. `git diff docs/openapi/` shows
your API changes, or nothing if the API did not change. Commit the file together with the code change.

The application connects to `localhost` (database `bug_report`, Artemis on `61616`, Mailpit on `1025`).
To use other values or another port, override the profile properties, for example:

```bash
mvn verify -Popenapi -DskipTests -Dopenapi.port=8090
```

Expected result: the same as above, using port `8090` for the temporary application. The other properties
are `openapi.datasource.url`, `openapi.artemis.url`, `openapi.mail.host` and `openapi.output.dir`.

Stop the infrastructure when you are done. Run from the repository root:

```bash
docker compose down
```

Expected result: the containers are removed (the database volume is kept).

## What CI runs

`.github/workflows/openapi.yml` runs on every pull request and on pushes to `master`. It starts the same
three containers, runs `mvn -B verify -Popenapi -DskipTests` in `backend/`, then runs
`git diff --exit-code docs/openapi/`. The check fails if the committed spec differs from the regenerated one.
The generated spec is uploaded as the `openapi-spec` artifact.
