"""
Minimal attacker endpoint. Listens on http://localhost:8888/ and pretty-prints
every inbound request to the terminal so the audience can see exactly what
the "helpful-build-tools" telemetry payload is actually sending.

This is the PRIMARY exfiltration target for the live demo. The whole flow
runs offline — no webhook.site, no venue WiFi dependency, and no chance of
a real secret leaving the laptop if someone forgets to unset their shell
env vars before a dry run.

Run:
    python3 server.py

In another terminal:
    cd demo/hello-spring-boot
    export ANTHROPIC_API_KEY="sk-ant-api03-DEMO-KEY-do-not-revoke"
    ./mvnw -q test
"""

from __future__ import annotations

import json
import sys
from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = 8888

GREEN = "\033[32m"
RED = "\033[31m"
BOLD = "\033[1m"
RESET = "\033[0m"


class Handler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b""
        body = raw.decode("utf-8", errors="replace")

        stamp = datetime.now().strftime("%H:%M:%S")
        print()
        print(f"{BOLD}{RED}[{stamp}] !!! PAYLOAD RECEIVED !!!{RESET}")
        print(f"  Path:   {self.path}")
        print(f"  From:   {self.client_address[0]}")
        print(f"  Length: {length} bytes")
        print(f"  {BOLD}Body:{RESET}")

        try:
            parsed = json.loads(body)
            # ensure_ascii=False so any unicode in the payload (e.g. "…") renders
            # literally on the projector instead of as "…".
            print(json.dumps(parsed, indent=2, ensure_ascii=False))
        except json.JSONDecodeError:
            print(body)

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"status":"ok"}')

    def log_message(self, format: str, *args: object) -> None:  # noqa: A002
        # Silence the default per-request log. We print our own.
        return


def main() -> int:
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"{GREEN}Listening for exfiltrated secrets on http://localhost:{PORT}/{RESET}")
    print("Press Ctrl+C to stop.\n")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
