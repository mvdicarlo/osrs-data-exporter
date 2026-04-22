# OSRS Data Exporter

![CI](https://github.com/mvdicarlo/osrs-data-exporter/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/github/license/mvdicarlo/osrs-data-exporter)
![Java](https://img.shields.io/badge/Java-11%2B-blue)
![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-orange)

A [RuneLite](https://runelite.net/) plugin that exports OSRS account data to configurable storage targets.

## Features

- **Bank Export** — Snapshots bank contents whenever the bank is updated
- **Inventory Export** — Snapshots inventory contents whenever the inventory changes
- **Skills Export** — Snapshots all skill levels and XP whenever a stat changes (5 second debounce)
- **Group Storage Export** — Snapshots Group Ironman shared storage whenever it is updated
- **Local Storage** — Writes JSON files to `~/.runelite/osrs-data-exporter/{accountHash}/`
- **Azure Blob Storage** — Uploads payload JSON files to an Azure Storage container using a connection string
- **Smart Debounce** — Rapid changes are coalesced into a single export (2s for items, 5s for skills)
- **Seasonal World Filter** — Automatically disables exports on Leagues, Deadman, Tournament, Fresh Start, and nosave beta worlds
- **Adapter/Factory Pattern** — Extensible architecture for adding new export targets

## Export Targets

| Target | Status | Description |
|---|---|---|
| Local Storage | Available | Writes JSON files to `~/.runelite/osrs-data-exporter/{accountHash}/` |
| Azure Blob Storage | Available | Uploads blobs under `osrs-data-exporter/{accountHash}/{dataType}.json` |

## Configuration

Found under the **OSRS Data Exporter** section in RuneLite settings:

| Setting | Section | Default | Description |
|---|---|---|---|
| Export Bank Data | Data Sources | Enabled | Export bank snapshot on each update |
| Export Inventory Data | Data Sources | Enabled | Export inventory snapshot on each update |
| Export Skills Data | Data Sources | Enabled | Export skills snapshot on each stat change |
| Export Group Storage Data | Data Sources | Enabled | Export GIM shared storage snapshot on each update |
| Enable Local Storage | Export Targets | Enabled | Write JSON to the local `.runelite` directory |
| Enable Azure Blob Storage | Export Targets | Disabled | Upload JSON payloads to an Azure Blob container |
| Azure Blob Connection String | Export Targets | Empty | Azure Storage connection string (secret) |
| Azure Blob Container | Export Targets | `osrs-data-exporter` | Container name used for uploaded blobs |

## Output Format

Each export target writes data per account. For local storage, files are at:

```
~/.runelite/osrs-data-exporter/
  └── {accountHash}/
      ├── bank.json
      ├── inventory.json
      ├── group-storage.json
      └── skills.json
```

Example `bank.json`:

```json
{
  "dataType": "BANK",
  "record": {
    "accountHash": 123456789,
    "timestamp": "2026-04-21T12:00:00Z",
    "items": [
      {
        "itemId": 4151,
        "itemName": "Abyssal whip",
        "quantity": 1,
        "members": true,
        "tradeable": true,
        "price": 2560
      },
      {
        "itemId": 995,
        "itemName": "Coins",
        "quantity": 50000,
        "members": false,
        "tradeable": true,
        "price": 1
      }
    ]
  }
}
```

> **Note:** `price` is the base store price in coins. High alch = `floor(price × 0.6)`, low alch = `floor(price × 0.4)`.

### JSON Schemas

Machine-readable JSON Schemas ([draft-07](http://json-schema.org/draft-07/schema)) are provided for all exported files. Use them for validation, IDE autocomplete, or code generation:

| Output File | Schema |
|---|---|
| `bank.json` | [schema/bank.json](schema/bank.json) |
| `inventory.json` | [schema/inventory.json](schema/inventory.json) |
| `group-storage.json` | [schema/group-storage.json](schema/group-storage.json) |
| `skills.json` | [schema/skills.json](schema/skills.json) |

## Building

Requires Java 11+ (Java 21 recommended for running Gradle 8.10):

```bash
./gradlew build
```

## Running (Development)

Launches RuneLite with the plugin loaded in developer mode:

```bash
./gradlew run
```

## License

[BSD 2-Clause](LICENSE)
