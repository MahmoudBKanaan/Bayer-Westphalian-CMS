# Bayer-Westphalian Campaign Management Platform — backend image
# Sprint 17 item 685 — Docker backend image build
#
# Multi-stage: Maven package (Java 21) → slim JRE runtime. No production secrets baked in.

# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Copy build metadata and sources (lock Maven deps when pom changes).
COPY pom.xml .
COPY config ./config
COPY src ./src

# Package Spring Boot fat JAR; tests run in dedicated CI jobs (679–680), not here.
RUN mvn -B -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user for container runtime. su-exec drops privileges after volume chown.
RUN apk add --no-cache su-exec \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /app/data/consent-evidence \
    && chown -R spring:spring /app

# finalName from pom.xml: bayer-westphalian-campaign-platform.jar
COPY --from=build /workspace/target/bayer-westphalian-campaign-platform.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh \
    && chown spring:spring /app/app.jar /app/docker-entrypoint.sh

# Entrypoint runs as root only long enough to fix consent-evidence volume ownership,
# then permanently drops to spring via su-exec (see docker-entrypoint.sh).
USER root
EXPOSE 8080

# Profile and secrets supplied at runtime (SPRING_PROFILES_ACTIVE, JWT_SECRET, DB_*, etc.).
ENTRYPOINT ["/app/docker-entrypoint.sh"]
