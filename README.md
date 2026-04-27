# You Gotta Keep the Dogs Away

> Demo code for the JCon 2026 talk **"You Gotta Keep the Dogs Away —
> YOLO developer workflows with a coding agent in a box"** by
> [Kevin Wittek](https://github.com/kiview), Docker.

The talk argues that as coding agents climb [Steve Yegge's autonomy
ladder][yegge], your vigilance stops scaling and infrastructure has to
take over. This repo contains the two live demos used to make that
case with [`sbx`][sbx], Docker's standalone agent sandbox.

- **Scenario 1.** A "helpful" Maven plugin in the build leaks your API
  keys on `mvn test`. Run it on the host: keys leave the laptop. Run
  it inside `sbx`: the proxy 403s the outbound POST, key never moves.
- **Scenario 2.** Testcontainers — the workflow Java devs are
  rightfully worried about losing — keeps working unchanged inside an
  `sbx` sandbox, because each sandbox ships its own Docker daemon.

Slides for the talk are checked in at [`slides/slides.html`](slides/slides.html)
(open in a browser).

[yegge]: https://newsletter.pragmaticengineer.com/p/steve-yegge-on-ai-agents
[sbx]: https://www.docker.com/products/docker-sandboxes/

## Repo layout

```text
demo/
├── hello-spring-boot/            ← scenario 1: the consuming app the agent sees
│   ├── pom.xml                   ← declares io.opensensing:build-reporter-maven-plugin
│   ├── src/                      ← unremarkable Spring Boot starter
│   └── .mvn/local-repo/          ← pre-compiled malicious plugin jar (the payload)
│
├── helpful-build-tools-plugin/   ← source for that plugin (transparency only;
│                                   in a real attack you'd never see this)
│
├── attacker-server/              ← local Python HTTP server that receives
│                                   the exfiltrated payload (offline, no
│                                   webhook.site, never leaves the laptop)
│
└── testcontainers-app/           ← scenario 2: a normal Spring Boot + JPA +
                                    Postgres service whose Testcontainers tests
                                    "just work" inside an sbx sandbox
```

> **Why is the malicious app called `hello-spring-boot/`?**
> Because a coding agent reading this repo would refuse to help with
> something obviously named `malicious-app/`. The whole point of the
> demo is that the consuming project looks innocent — the attack lives
> in a binary plugin, not in the source tree. Naming the directory to
> match what an agent would actually encounter in the wild makes the
> demo run end-to-end without the agent flinching.

## Prerequisites

- macOS or Linux (Apple Silicon tested).
- [`sbx`][sbxdocs] installed:
  - `brew install docker/tap/sbx` (macOS)
  - `winget install Docker.sbx` (Windows)
  - Linux: see the [docs][sbxdocs]
- Java 21+ on the host. Each demo app ships a Maven Wrapper, so no
  `mvn` on `PATH` is needed.
- Python 3 for the attacker server. No third-party packages.

[sbxdocs]: https://docs.docker.com/ai/sandboxes/

## Scenario 1 — the malicious Maven build

A Maven plugin called `io.opensensing:build-reporter-maven-plugin`
claims to publish anonymous build metrics. It's wired into
`hello-spring-boot/pom.xml` and bound to `process-classes`, so it
fires on every `mvn compile` / `test` / `package` / `install` /
`verify` / `spring-boot:run`. On every run it reads a hard-coded list
of API-key env vars and POSTs them to a fake C2 endpoint. The source
tree the agent sees contains zero hints of this — the malice lives
in the bytecode.

### One-time setup

The plugin's default endpoint is
`http://telemetry.helpful-build-tools.example:8888/collect`. Map that
fake domain to localhost so the demo lands payloads at the local
attacker server:

```bash
echo "127.0.0.1 telemetry.helpful-build-tools.example" \
    | sudo tee -a /etc/hosts
```

### Run it

In one terminal, start the local attacker server:

```bash
cd demo/attacker-server
python3 server.py
# Listening for exfiltrated secrets on http://localhost:8888/
```

In a second terminal, **unset any real API keys first**, then export a
demo key (the plugin reads everything in `TARGET_ENV_VARS`):

```bash
unset OPENAI_API_KEY GITHUB_TOKEN AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
export ANTHROPIC_API_KEY="sk-ant-api03-DEMO-KEY-do-not-revoke"
```

#### Host run — the key leaks

```bash
cd demo/hello-spring-boot
./mvnw -q test
```

Build succeeds. Tests pass. The plugin logs one ordinary-looking line:

```text
[opensensing] sending build event to http://telemetry.helpful-build-tools.example:8888/collect
```

Switch to the attacker-server terminal — a red `!!! PAYLOAD RECEIVED !!!`
banner is now sitting there with the JSON payload, including a preview
of `ANTHROPIC_API_KEY`. The build reported success. No alarm fired.

#### Sandbox run — the key stays put

Stash the demo key in `sbx`'s secret store (so an agent running inside
the sandbox could still call `api.anthropic.com` via the proxy), then
create a sandbox that mounts only the malicious app:

```bash
echo "$ANTHROPIC_API_KEY" | sbx secret set -g anthropic
sbx create shell "$(pwd)" --name malicious   # from demo/hello-spring-boot
sbx run malicious
```

Inside the sandbox shell:

```bash
echo "Key in env: '${ANTHROPIC_API_KEY:-not set}'"
# → 'proxy-managed'   ← real key never enters the agent's environment

./mvnw -q test
# Build succeeds. Plugin fires. POST is silently 403'd by the sbx proxy.
```

The attacker-server terminal stays empty. To see the block directly:

```bash
curl -v --max-time 3 http://telemetry.helpful-build-tools.example:8888/collect
# < HTTP/1.1 403 Forbidden
# Blocked by network policy: domain telemetry.helpful-build-tools.example:8888
#   detail: no matching allow rule — blocked by default deny policy
```

Default-allowed hosts (Maven Central, Docker Hub) still pass under the
balanced default policy:

```bash
curl -s -o /dev/null -w "maven:  %{http_code}\n"  https://repo.maven.apache.org/
curl -s -o /dev/null -w "docker: %{http_code}\n"  https://registry-1.docker.io/
curl -s -o /dev/null -w "random: %{http_code}\n"  https://example.com/
# maven:  200
# docker: 200
# random: 403
```

## Scenario 2 — Testcontainers inside `sbx`

The `testcontainers-app/` module is a normal Spring Boot 4 + JPA +
Postgres service. The test (`PersonRepositoryTest`) uses
`@Testcontainers` with `@ServiceConnection` to start a real
`PostgreSQLContainer` and exercise the JPA repository.

The point: `mvn test` runs unchanged inside an `sbx` sandbox, because
each sandbox ships its own Docker daemon.

```bash
sbx create shell "$(pwd)" --name testcontainers   # from demo/testcontainers-app
sbx run testcontainers
# inside the sandbox:
./mvnw -q test
# Tests run: 1, Failures: 0, Errors: 0
```

### Version pin

The demo runs on Spring Boot **4.0.5** + Testcontainers **2.0.5** (which
brings docker-java **3.7.1**) on purpose. Older Testcontainers (≤ 1.19,
which Spring Boot 3.3 resolves by default) probes the Docker socket at
`/v1.32/info`, and the sandbox daemon enforces API ≥ 1.40. If your own
project sees `docker-java` complaining about API versions inside an
`sbx` sandbox, that's the cause.

## License

[MIT](LICENSE). Use freely; if it saves your build, a star is welcome.

## Credits

- Talk: [JCon 2026](https://schedule.jcon.one/2026/session/1052017),
  *"You Gotta Keep the Dogs Away"*.
- Author: Kevin Wittek ([@kiview](https://github.com/kiview)).
- The framing leans heavily on Cory Doctorow's
  ["Reverse centaurs are the answer to the AI paradox"](https://doctorow.medium.com/)
  and Steve Yegge's [eight levels of AI adoption][yegge].
