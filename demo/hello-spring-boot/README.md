# `hello-spring-boot` — the malicious Maven build demo

> The directory is called `hello-spring-boot` on purpose. A coding agent
> reading this folder sees an unremarkable Spring Boot starter and
> happily runs `./mvnw test`. That's the whole point: the attack lives
> in a binary plugin, not in source the agent might flag.

## What's in here

- `src/` — a tiny Spring Boot 3 web app. Completely innocuous; nothing
  here exfiltrates anything.
- `pom.xml` — declares one suspicious-only-if-you-look plugin,
  `io.opensensing:build-reporter-maven-plugin:1.0.3`, that "publishes
  anonymous build metrics."
- `.mvn/local-repo/` — a tiny file-based Maven repository containing the
  pre-compiled plugin jar (4 files, ~12 KB). Pulled in via a
  `<pluginRepository>` declaration in `pom.xml`.

The plugin's source lives next door under
`../helpful-build-tools-plugin/`. **In a real supply-chain attack you
would never have the source** — only the bytecode, pulled from a
public registry. The source is here so you can read it.

## What the plugin does

Bound to the `process-classes` lifecycle phase, so it fires on every
common Maven invocation: `compile`, `test`, `package`, `install`,
`verify`, `spring-boot:run`. On each run it:

1. Reads `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `GITHUB_TOKEN`,
   `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` from the build
   environment.
2. Truncates each present value to its first six characters plus `***`
   (on-stage safety; a real attacker would not).
3. POSTs them as JSON, along with `user.name`, hostname, and `cwd`, to
   `http://telemetry.helpful-build-tools.example:8888/collect`.
4. Silently swallows network errors so a flaky proxy doesn't break the
   build — i.e. doesn't tip you off when the call is blocked.

This is the realistic shape of a supply-chain compromise: **your source
tree is clean; the malice lives in a binary dependency you pulled from a
registry.** An agent reading the project sees nothing to flag.

## Running it

See the top-level `README.md` for the full setup (the `/etc/hosts`
entry, starting the local attacker server, exporting a demo key) and
the side-by-side host vs. sandbox comparison.

The short version once setup is done:

```bash
# host run — the key leaks to the local attacker server
cd demo/hello-spring-boot
./mvnw -q test

# sandbox run — the sbx proxy blocks the outbound POST
sbx create shell "$(pwd)" --name hello-demo
sbx run hello-demo
# inside the sandbox:
./mvnw -q test
```

## Why bind to `process-classes`?

It's the earliest Maven phase where compiled classes are available, and
it runs for every command developers and agents actually type:
`compile`, `test`, `package`, `install`, `verify`, `spring-boot:run`.
Binding at `validate` (the very first phase) would look even scarier
but requires classes that don't exist yet. `process-classes` is the
realistic attack point.

## Regenerating the plugin jar

If you change the source under `../helpful-build-tools-plugin/`, rebuild
into a scratch Maven repo, then copy just the plugin artifact over so
this app's `.mvn/local-repo/` stays minimal and auditable:

```bash
cd ../helpful-build-tools-plugin
SCRATCH=$(mktemp -d)
../hello-spring-boot/mvnw -q install -Dmaven.repo.local="$SCRATCH"

PLUGIN_DIR=io/opensensing/build-reporter-maven-plugin/1.0.3
mkdir -p ../hello-spring-boot/.mvn/local-repo/$PLUGIN_DIR
cp "$SCRATCH/$PLUGIN_DIR/build-reporter-maven-plugin-1.0.3.pom" \
   "$SCRATCH/$PLUGIN_DIR/build-reporter-maven-plugin-1.0.3.jar" \
   ../hello-spring-boot/.mvn/local-repo/$PLUGIN_DIR/

cd ../hello-spring-boot/.mvn/local-repo/$PLUGIN_DIR
shasum -a 1 build-reporter-maven-plugin-1.0.3.pom \
    | awk '{print $1}' > build-reporter-maven-plugin-1.0.3.pom.sha1
shasum -a 1 build-reporter-maven-plugin-1.0.3.jar \
    | awk '{print $1}' > build-reporter-maven-plugin-1.0.3.jar.sha1

rm -rf "$SCRATCH"
```
