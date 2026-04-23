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
- **Equipment Export** — Snapshots worn gear with full combat and defence stats whenever equipment changes
- **Grand Exchange Export** — Records GE offer state whenever an offer is placed, completed, or cancelled (all 8 slots reflected per write)
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
| Export Equipment Data | Data Sources | Enabled | Export worn gear with combat stats on each equipment change |
| Export Grand Exchange Data | Data Sources | Enabled | Export GE offer state on place, complete, or cancel |
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
      ├── skills.json
      ├── equipment.json
      └── grand-exchange.json
```

Example `bank.json`:

```json
{
  "dataType": "BANK",
  "record": {
    "account": {
      "accountHash": 123456789,
      "characterName": "Zezima"
    },
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

Example `equipment.json`:

```json
{
  "dataType": "EQUIPMENT",
  "record": {
    "account": {
      "accountHash": 123456789,
      "characterName": "Zezima"
    },
    "timestamp": "2026-04-21T12:00:00Z",
    "items": [
      {
        "itemId": 6570,
        "itemName": "Fire cape",
        "quantity": 1,
        "members": true,
        "tradeable": false,
        "price": 0,
        "stats": {
          "attackStab": 0,
          "attackSlash": 0,
          "attackCrush": 0,
          "attackMagic": 1,
          "attackRanged": 0,
          "defenceStab": 11,
          "defenceSlash": 11,
          "defenceCrush": 11,
          "defenceMagic": 11,
          "defenceRanged": 11,
          "meleeStrength": 4,
          "rangedStrength": 0,
          "magicDamage": 0.0,
          "prayer": 2,
          "attackSpeed": 0,
          "slot": 1,
          "twoHanded": false
        }
      }
    ]
  }
}
```

> **Note:** `magicDamage` is a float representing the bonus as a decimal (e.g. `0.1` = +10%). `slot` follows the equipment slot indices used by the game engine (1 = cape, 3 = weapon, 4 = body, etc.).

Example `grand-exchange.json`:

```json
{
  "dataType": "GRAND_EXCHANGE",
  "record": {
    "account": {
      "accountHash": 123456789,
      "characterName": "Zezima"
    },
    "timestamp": "2026-04-23T10:00:00Z",
    "offers": [
      {
        "slot": 0,
        "itemId": 4151,
        "itemName": "Abyssal whip",
        "state": "BOUGHT",
        "buy": true,
        "price": 2000000,
        "totalQuantity": 1,
        "quantityTraded": 1,
        "spent": 1850000
      },
      {
        "slot": 2,
        "itemId": 995,
        "itemName": "Coins",
        "state": "SELLING",
        "buy": false,
        "price": 1,
        "totalQuantity": 10000,
        "quantityTraded": 0,
        "spent": 0
      }
    ]
  }
}
```

> **Note:** `offers` contains all slots with a known state at the time of the snapshot. Empty slots are omitted. `quantityTraded` and `spent` reflect the amount transacted so far (final values at terminal states). `state` is one of: `BUYING`, `SELLING`, `BOUGHT`, `SOLD`, `CANCELLED_BUY`, `CANCELLED_SELL`.

### JSON Schemas

Machine-readable JSON Schemas ([draft-07](http://json-schema.org/draft-07/schema)) are provided for all exported files. Use them for validation, IDE autocomplete, or code generation:

| Output File | Schema |
|---|---|
| `bank.json` | [schema/bank.json](schema/bank.json) |
| `inventory.json` | [schema/inventory.json](schema/inventory.json) |
| `group-storage.json` | [schema/group-storage.json](schema/group-storage.json) |
| `skills.json` | [schema/skills.json](schema/skills.json) |
| `equipment.json` | [schema/equipment.json](schema/equipment.json) |
| `grand-exchange.json` | [schema/grand-exchange.json](schema/grand-exchange.json) |

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
