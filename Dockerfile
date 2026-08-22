# Stage 1: Build quarkus
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-25 AS gradle

WORKDIR /app
COPY . .

# Build native image
RUN ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false

# Stage 2: Quarkus micro image
FROM quay.io/quarkus/quarkus-micro-image:2.0
ARG PROJECT=mastodon-streaming
ARG VERSION=0.5.0
ARG PORT=8080

# Copy the Quarkus runner
WORKDIR /app
COPY --from=gradle "/app/build/*-runner" "/app/mastodon"

RUN chown 1001 /app && chmod "g+rwX" /app && chown 1001:root /app

VOLUME ["/app/jwt"]
EXPOSE $PORT
USER 1001
ENV QUARKUS_PROFILE=prod

# Run Quarkus app
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/app/mastodon"]
