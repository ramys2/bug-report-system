#!/bin/bash

# Starts the backend with `mvn spring-boot:run` from backend/bug-report-api.
#
# Usage:        ./run-backend.sh          start the backend
#               ./run-backend.sh build    run build-backend.sh first (also installs the domain module,
#                                         which bug-report-api needs), then start
# Notes:        Run it from the repository root. With two or more arguments it prints a message and
#               exits; a single argument other than "build" is ignored.
# Requires:     JDK 21 and Maven, plus the services from docker-compose.yml. application.yml points to the
#               hosts `database`, `artemis` and `mailpit` (the docker-compose service names).
#               TODO(verify): how these host names resolve when the backend runs directly on the host.

if [ $# -ge 2 ]; then
    echo "Only one argument allowed!"
    exit
fi

if [ "$1" = "build" ]; then
    bash build-backend.sh
fi

cd backend/bug-report-api

mvn spring-boot:run
