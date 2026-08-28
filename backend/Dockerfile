FROM maven:3.9.16-amazoncorretto-21-debian-trixie as build

COPY ./ /tmp/bug-report-build/

WORKDIR /tmp/bug-report-build/

RUN mvn package
RUN mv ./bug-report-api/target/bug-report-api-0.1.0-SNAPSHOT.jar ./
