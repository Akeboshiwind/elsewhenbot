FROM clojure:temurin-21-tools-deps-bookworm-slim AS builder

WORKDIR /app

COPY deps.edn .
RUN clj -P -X:build

COPY src/ /app/src
COPY build.clj .
RUN clj -T:build uber


FROM eclipse-temurin:21 AS jdk-base
FROM debian:bookworm-slim AS runner

# Copy over JDK
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=jdk-base $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

# Copy over the built jar
COPY --from=builder /app/target/elsewhenbot-*-standalone.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
