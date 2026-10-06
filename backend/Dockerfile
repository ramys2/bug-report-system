FROM maven:3.9.16-amazoncorretto-21-debian-trixie AS build

WORKDIR /build

COPY . .

RUN mvn package -DskipTests


FROM eclipse-temurin:21-jre-ubi10-minimal AS runtime

WORKDIR /opt/app

COPY --from=build /build/bug-report-api/target/app.jar ./app.jar

CMD ["java", "-jar", "app.jar"]