# Local attacker server

A standalone Python 3 HTTP server that receives the exfiltrated payload
from the `build-reporter-maven-plugin` (source under
`../helpful-build-tools-plugin/`) and pretty-prints it to the terminal so
you can see exactly what a "helpful" build plugin can leak.

Running everything locally means:

- **No secret leaves the laptop.** Even the demo key is safer.
- **No network dependency.** Works offline / on a plane.
- **Terminal output is easy to read** — the server prints a red banner
  per POST, visible from across the room.

## Run it

```bash
python3 server.py
```

Binds `0.0.0.0:8888`. Requires only Python 3 — no third-party
dependencies.

## How the demo points at it

The plugin's default endpoint is
`http://telemetry.helpful-build-tools.example:8888/collect`. That fake
domain resolves to `127.0.0.1` via a one-line `/etc/hosts` entry (see the
top-level `README.md`), so a plain `./mvnw test` in
`../hello-spring-boot/` POSTs straight to this server.

## What it shows in each step of the demo

| Step | Outcome on this server |
|------|------------------------|
| Host run (no sandbox) | Red banner + full JSON payload with the leaked key preview |
| Sandbox run (default policy) | **Nothing.** The sbx proxy refuses the outbound POST with `403 Blocked by network policy`. Payload never leaves the sandbox. |

## Payload shape

The plugin reads a hard-coded list of env vars (`ANTHROPIC_API_KEY`,
`OPENAI_API_KEY`, `GITHUB_TOKEN`, `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`), truncates each present value to the first six
characters plus `***` for on-stage safety, and POSTs them as JSON
together with `user.name`, hostname, and `cwd`. A real attacker would
ship the full values — this demo deliberately doesn't.
