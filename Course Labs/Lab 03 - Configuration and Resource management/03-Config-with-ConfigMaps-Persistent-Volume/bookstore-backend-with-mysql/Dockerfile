# Build Stage for Spring boot application image
FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-alpine-slim as build

LABEL maintainer="m.romdhani@businesstraining.be"

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Production Stage for Spring boot application image
FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency

# Copy the dependency application file from build stage artifact
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

CMD java -cp app:app/lib/* be.businesstraining.BookstoreBackendApplication