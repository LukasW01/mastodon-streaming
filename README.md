# Mastodon (Streaming)

A blazingly fast, memory-efficient drop-in replacement for the Mastodon streaming API server.

## Getting Started

### Prerequisites

- [Java 25 (GraalVM)](https://www.graalvm.org/downloads/)
- [Gradle](https://gradle.org/install/)
- [Docker](https://docs.docker.com/get-docker/)

### Installation

Build the application:

```bash
gradle build
```

Run the application:

```bash
gradle quarkusDev
```

## Configuration

The server is configured via environment variables. For local development, see an example in `.env.example`.

### Required

| Variable            | Description                                                                            |
|---------------------|----------------------------------------------------------------------------------------|
| `POSTGRES_USER`     | PostgreSQL user                                                                        |
| `POSTGRES_PASSWORD` | PostgreSQL password                                                                    |
| `POSTGRES_JDBC_URL` | Full JDBC URL, e.g. `jdbc:postgresql://localhost:5432/mastodon_production`<sup>1</sup> |
| `REDIS_URL`         | Full Redis URL, e.g. `redis://localhost:6379/0`<sup>2</sup>                            |

<sup>1</sup> Alternatively, leave `POSTGRES_JDBC_URL` unset and set `POSTGRES_HOST` and `POSTGRES_DB` (plus optionally `POSTGRES_PORT`).
`POSTGRES_JDBC_URL` takes precedence if both are set.

<sup>2</sup> Alternatively, leave `REDIS_URL` unset and set `REDIS_HOST` (plus optionally `REDIS_PORT`, `REDIS_DB` and `REDIS_USER`).
`REDIS_URL` takes precedence if both are set.

### Optional

| Variable          | Default     | Description                                                                         |
|-------------------|-------------|-------------------------------------------------------------------------------------|
| `QUARKUS_PROFILE` | `prod`      | Configuration profile: `prod`, `dev` or `test` (`gradle quarkusDev` uses `dev`)     |
| `BIND`            | `127.0.0.1` | Address the HTTP server binds to (`0.0.0.0` in the Docker image)                    |
| `PORT`            | `4000`      | Port the HTTP server listens on                                                     |
| `LOG_LEVEL`       | `INFO`      | Root log level (the `dev` profile additionally logs the application at `DEBUG`)     |
| `DB_POOL`         | `10`        | Maximum number of PostgreSQL connections in the pool                                |
| `POSTGRES_HOST`   |             | PostgreSQL host, used when `POSTGRES_JDBC_URL` is not set                           |
| `POSTGRES_PORT`   | `5432`      | PostgreSQL port, used when `POSTGRES_JDBC_URL` is not set                           |
| `POSTGRES_DB`     |             | PostgreSQL database name, used when `POSTGRES_JDBC_URL` is not set                  |
| `REDIS_HOST`      |             | Redis host, used when `REDIS_URL` is not set                                        |
| `REDIS_PORT`      | `6379`      | Redis port, used when `REDIS_URL` is not set                                        |
| `REDIS_DB`        | `0`         | Redis database index, used when `REDIS_URL` is not set                              |
| `REDIS_USER`      |             | Redis user (ACL), used when `REDIS_URL` is not set                                  |
| `REDIS_PASSWORD`  |             | Redis password                                                                      |
| `REDIS_NAMESPACE` |             | Prefix for Redis channels, must match Mastodon's `REDIS_NAMESPACE` if one is used   |

Any other Quarkus property can be overridden with its environment variable equivalent.
See the [Quarkus configuration guide](https://quarkus.io/guides/config-reference#environment-variables) for details.

## Qodana

https://qodana.cloud/projects/y7noE

## License

This program is licensed under the MIT-License. See the "LICENSE" file for more information
