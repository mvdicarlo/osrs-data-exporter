You are working on the OSRS Data Exporter, a RuneLite plugin that exports OSRS account data to configurable storage targets.

## Core Principles

- **Performance First** — All actions must be performant. No heavy memory strain, UI stuttering, or blocking the client thread. Export I/O always runs on background threads.
- **No External Libraries** — Only use libraries pre-approved by RuneLite (already on the classpath). No additional dependencies beyond what RuneLite provides, except standard test libraries (JUnit, Mockito).
- **Clean Code** — Well-documented, object-oriented code. Small, descriptive methods following good method composition. No free-floating strings — prefer constants and enums.
- **Adapter/Factory Pattern** — Exporters are adapters created by a factory (`DataExporterFactory`). Each adapter implements `DataExporter` and is keyed by `ExportType`. This enables future export targets without modifying existing code.
- **Smart Debounce** — Rapid item container changes (e.g. depositing multiple items quickly) are coalesced into a single export after a quiet period. Each data type (bank, inventory) has its own independent debounce.
- **Seasonal World Filtering** — Data export is disabled on seasonal/temporary worlds (Leagues, Deadman, Tournament, Fresh Start, nosave beta) since that data is non-permanent.
- **Account Isolation** — All exported data is scoped per account using `client.getAccountHash()`. Unauthenticated sessions (hash == -1) are ignored.

## Architecture

- **Models** — `ExportRecord` (abstract base with accountHash + timestamp) → `BankRecord`, `InventoryRecord`. Shared `ItemEntry` for item data. `ExportPayload<T>` wraps a `DataType` enum + record.
- **Exporters** — `DataExporter` interface → `LocalStorageExporter` (writes JSON to `~/.runelite/osrs-data-exporter/{accountHash}/`). Factory creates exporters based on config.
- **Plugin** — Listens to `ItemContainerChanged` events, routes by `InventoryID`, snapshots data on client thread, dispatches via debounced background executor.
- **Config** — `OsrsDataExporterConfig` with sections for Data Sources (toggles per data type) and Export Targets (toggles per adapter).

## Build & Test

- **Template**: RuneLite Plugin Hub template (Gradle 8.10, Java 11 target, `latest.release` RuneLite version)
- **Build**: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home && ./gradlew build`
- **Run**: `./gradlew run` (launches RuneLite in dev mode with `--add-exports` for macOS compatibility)
- **Tests**: JUnit 4 + Mockito. `LocalStorageExporter` accepts injected `baseDir` for testability (no reflection hacks). Gson `Instant` serialization handled via TypeAdapter in tests.

## Conventions

- RuneLite tab-indented style (tabs, not spaces)
- Lombok annotations: `@Value` for immutable data, `@Data` for mutable, `@Slf4j` for logging, `@EqualsAndHashCode(callSuper = true)` for subclasses
- BSD 2-Clause license
