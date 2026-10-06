#!/bin/bash

# Runs the backend tests in two steps: the unit and controller tests (`mvn test`, no database
# needed), then the integration tests (`mvn verify`, which runs all tests again).
#
# Usage:        ./run-tests.sh   (run from the repository root, because of the relative `cd backend`)
# Requires:     JDK 21, Maven and Docker with the Compose plugin. The integration tests need the
#               bug_report_test database, so the script starts `database` and runs the
#               one-shot `database-test-init` job (see docker-compose.yml) before them.

set -e

cd backend

mvn test

docker compose run --rm database-test-init

mvn verify
