#!/bin/bash

# Builds the backend: runs `mvn clean install` in backend/, which compiles both
# modules, runs their tests and installs them into the local Maven repository.
#
# Usage:        ./build-backend.sh   (run from the repository root, because of the relative `cd backend`)
# Requires:     JDK 21 and Maven. The tests need a MariaDB reachable at localhost:3306 with the
#               bug_report_test database (see database/init/ and docker-compose.yml).

cd backend

mvn clean install
