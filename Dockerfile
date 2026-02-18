# Stage 1: Build quarkus
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS gradle

WORKDIR /app
COPY . .

# Build native image
RUN ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false

# Stage 2: Quarkus micro image
FROM quay.io/quarkus/quarkus-micro-image:2.0
ARG PROJECT=mastodon
ARG VERSION=0.5.0
ARG PORT=8080

WORKDIR /app

# Copy the Quarkus runner
COPY --from=gradle "/app/build/*-runner" "/app/mastodon"

VOLUME ["/app/jwt"]

RUN chown 1001 /app && chmod "g+rwX" /app && chown 1001:root /app

EXPOSE $PORT
USER 1001

# Run Quarkus app
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/app/mastodon"]
