# RedisFlagd

RedisFlagd is an implementation of Flagd's [FlagSyncService gRPC interface](https://github.com/open-feature/schemas/blob/main/protobuf/sync/v1/sync_service.proto) that uses Redis to store flag configurations.

Flag configurations are stored in a Redis hash with key `flagd:flags`. The field is the name of the feature flag and the value is its configuration (e.g. `{"state": "ENABLED", "variants": {"on": true, "off": false}, "defaultVariant": "on"}`).

RedisFlagd relies on Redis [keyspace notifications](https://redis.io/docs/manual/keyspace-notifications/) to notify it of updates. Your Redis server should be configured with a `notify-keyspace-events` of at least `Kh`.

## Building a container image

To build a container image, you need Docker and a Java development kit (version 17 or greater) installed. Run:
```sh
./gradlew dockerBuildNative
```

## Environment Variables

You can configure RedisFlagd via the following environment variables:

| Environment Variable | Description |
| --- | --- |
| `REDIS_URI` | URI of Redis server with your flag configurations, e.g. `redis://flagd-redis.production:6379` |
| `GRPC_SERVER_PORT` | port for gRPC server to listen on; defaults to 50051 |

## Running tests

To run tests, you can use IntelliJ IDEA and the [Kotest plugin](https://plugins.jetbrains.com/plugin/14080-kotest).

Alternatively, you can run tests from the command line:
```sh
./gradlew test
```
