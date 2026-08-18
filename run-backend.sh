#!/bin/bash

if [ $# -ge 2 ]; then
    echo "Only one argument allowed!"
    exit
fi

if [ "$1" = "build" ]; then
    bash build-backend.sh
fi

cd backend/bug-report-api

mvn spring-boot:run
