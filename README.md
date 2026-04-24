# OSRS Data Exporter

![CI](https://github.com/mvdicarlo/osrs-data-exporter/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/github/license/mvdicarlo/osrs-data-exporter)
![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-orange)

A [RuneLite](https://runelite.net/) plugin that automatically exports your OSRS account data to configurable storage targets whenever it changes in-game.

## Features

- **Bank** — Snapshots bank contents on every update
- **Inventory** — Snapshots inventory contents on every change
- **Equipment** — Snapshots worn gear with full combat/defence stats on every gear change
- **Skills** — Snapshots all skill levels and XP on every stat change
- **Group Storage** — Snapshots Group Ironman shared storage on every update
- **Grand Exchange** — Records all GE slot states whenever an offer is placed, completed, or cancelled
- **Smart Debounce** — Rapid changes (e.g. depositing items quickly) are coalesced into a single export
- **Seasonal World Filter** — Exports are automatically disabled on Leagues, Deadman, Tournament, Fresh Start, and nosave beta worlds

## Export Targets

| Target | Default | Description |
|---|---|---|
| Local Storage | Enabled | Writes JSON files to `~/.runelite/osrs-data-exporter/{accountHash}/` |
| Azure Blob Storage | Disabled | Uploads JSON to an Azure Storage container via connection string |

## Configuration

All settings are under **OSRS Data Exporter** in RuneLite settings.

### Data Sources

Toggle which data types are exported. All are enabled by default.

### Export Targets

| Setting | Default | Description |
|---|---|---|
| Enable Local Storage | Enabled | Write JSON to the local `.runelite` directory |
| Enable Azure Blob Storage | Disabled | Upload JSON to an Azure Blob container |
| Azure Blob Connection String | Empty | Azure Storage connection string (stored as a secret) |
| Azure Blob Container | `osrs-data-exporter` | Container name for uploaded blobs |

## Output

Each data type is written as a separate JSON file per account:

```
~/.runelite/osrs-data-exporter/
  └── {accountHash}/
      ├── bank.json
      ├── inventory.json
      ├── equipment.json
      ├── skills.json
      ├── group-storage.json
      └── grand-exchange.json
```

<details>
<summary>Example bank.json</summary>

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
      }
    ]
  }
}
```
</details>

<details>
<summary>Example equipment.json</summary>

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
</details>

<details>
<summary>Example grand-exchange.json</summary>

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
      }
    ]
  }
}
```
</details>

### Field Notes

- `price` — Base store price in coins. High alch = `floor(price × 0.6)`, low alch = `floor(price × 0.4)`
- `magicDamage` — Float representing the bonus as a decimal (e.g. `0.1` = +10%)
- `slot` (equipment) — Equipment slot index used by the game engine (1 = cape, 3 = weapon, 4 = body, etc.)
- `state` (GE) — One of: `BUYING`, `SELLING`, `BOUGHT`, `SOLD`, `CANCELLED_BUY`, `CANCELLED_SELL`
- `contents` (items) — Inner items for container-like items (e.g. Rune Pouch); `null` for regular items

### JSON Schemas

Machine-readable [JSON Schemas (draft-07)](http://json-schema.org/draft-07/schema) are provided for validation, IDE autocomplete, or code generation:

| Output File | Schema |
|---|---|
| `bank.json` | [schema/bank.json](schema/bank.json) |
| `inventory.json` | [schema/inventory.json](schema/inventory.json) |
| `group-storage.json` | [schema/group-storage.json](schema/group-storage.json) |
| `skills.json` | [schema/skills.json](schema/skills.json) |
| `equipment.json` | [schema/equipment.json](schema/equipment.json) |
| `grand-exchange.json` | [schema/grand-exchange.json](schema/grand-exchange.json) |

## License

[BSD 2-Clause](LICENSE)
