# RedisFlagd

RedisFlagd is an implementation of Flagd's [FlagSyncService gRPC interface](https://github.com/open-feature/flagd-schemas/blob/main/protobuf/flagd/sync/v1/sync.proto) that uses Redis to store flag configurations.

Flag configurations are stored in a Redis hash with key `flagd:flags`. The field is the name of the feature flag and the value is its configuration (e.g. `{"state": "ENABLED", "variants": {"on": true, "off": false}, "defaultVariant": "on"}`).

RedisFlagd relies on Redis [keyspace notifications](https://redis.io/docs/latest/develop/use/keyspace-notifications/) to notify it of updates. Your Redis server should be configured with a `notify-keyspace-events` of at least `Kh`.

There is a web app to manage feature flags: [RedisFlagd UI](https://github.com/tobyhs/redisflagd-ui).

## Container Image

A container image is pushed to [GitHub Packages](https://github.com/tobyhs/redisflagd/pkgs/container/redisflagd) at ghcr.io/tobyhs/redisflagd:latest after a release is published.

## Configuration

You can configure RedisFlagd via the following environment variables:

| Environment Variable | Description |
| --- | --- |
| `REDIS_URI` | URI of Redis server with your flag configurations, e.g. `redis://flagd-redis.production:6379` |
| `GRPC_SERVER_PORT` | port for gRPC server to listen on; defaults to 50051 |

## Development

To work on RedisFlagd, you should have a JDK installed. See the [.java-version](.java-version) file for the major version of Java to use.

To run RedisFlagd from the command line, run:
```sh
./gradlew run
```

To build a container image using GraalVM Native Image, run:
```sh
./gradlew dockerBuildNative
```

There is a [Docker Compose file](compose.yaml) that runs Redis, RedisFlagd, and flagd for development/testing purposes.

## Running tests

To run tests, you can use IntelliJ IDEA and the [Kotest plugin](https://plugins.jetbrains.com/plugin/14080-kotest).

Alternatively, you can run tests from the command line:
```sh
./gradlew test
```
