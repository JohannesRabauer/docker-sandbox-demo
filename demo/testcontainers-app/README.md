# Testcontainers-in-a-box demo app (Maven)

A small Spring Boot + JPA + Postgres service. The test
(`PersonRepositoryTest`) uses Testcontainers with `@ServiceConnection` to
start a real Postgres container and exercise the JPA repository.

The point of this demo is that `mvn test` runs unchanged inside an sbx
sandbox — the same Testcontainers workflow you already have, just on the
safe side of the boundary.

## Versions (verified on sbx v0.27.0, 2026-04-22)

- Spring Boot **4.0.5**
- Testcontainers **2.0.5** (pulls docker-java **3.7.1**)
- Java **21**
- Postgres **16-alpine**

Older combos (Spring Boot 3.3.x + Testcontainers 1.19.x / 1.20.x / 1.21.x)
fail inside the sandbox because the bundled docker-java probes the socket
at `/v1.32/info` and the sandbox daemon enforces API ≥ 1.40. Testcontainers
2.0.5 ships a docker-java (3.7.1) that speaks a current API version. Do
not downgrade.

## Demo commands

Baseline on the host:

```bash
cd demo/testcontainers-app
./mvnw -q test
```

Then inside sbx:

```bash
sbx run shell
cd /Users/<you>/.../demo/testcontainers-app
./mvnw -q test
```

The Maven Wrapper downloads Apache Maven 3.9.11 on first invocation
(from `repo.maven.apache.org`, which is on the default `balanced`
allow-list). No `apt-get install maven` step needed inside the sandbox.

## Prerequisites inside the sandbox

- `postgres:16-alpine` pulled into the sandbox's Docker daemon. Not
  shared with the host daemon. First run pulls it automatically.
- The workspace mounted at its host absolute path (the default for
  `sbx create shell .` and `sbx run shell`).

## Gotcha: first `docker pull` on ARM can produce a mis-tagged image

Observed once on the demo machine: the initial `docker pull
postgres:16-alpine` landed an image that `docker image inspect` reported
as `arm64` but whose binaries produced `exec /usr/local/bin/docker-entrypoint.sh:
exec format error`. Container died with exit code 255 in ~50ms.
Re-pulling with `--platform linux/arm64` fixed it:

```bash
docker rmi postgres:16-alpine
docker pull --platform linux/arm64 postgres:16-alpine
```

On Apple Silicon, pre-pull with `--platform linux/arm64` once after the
sandbox is created to avoid the surprise. Testcontainers itself is
fine — this is a daemon-side pull-time manifest-selection quirk.
