# Itemsmith

In-game item editor for Paper and Folia 1.21 and 26.x. Hold an item, type `/item` and edit everything
from a menu: name, description, enchantments, attributes, hide flags, model, tooltip style, rarity,
colour, player heads, durability and more. Every change can be undone.

## Installation

1. Drop `Itemsmith.jar` into `plugins/`.
2. Start the server. `config.yml` and the language files are created in `plugins/Itemsmith/`.

PlaceholderAPI is optional.

## Commands

| Command | Effect |
|---|---|
| `/item` | opens the editor for the item in your hand |
| `/item edit <field> ...` | edits a field directly (see `/item help`) |
| `/item undo` | undoes the last change |
| `/item info` | summarises the item in your hand |

Aliases: `/itemedit`, `/ie`, `/itemeditor`, `/edititem`.

`/item edit` fields: `name`, `lore`, `enchant`, `flag`, `attribute`, `amount`, `maxstack`, `damage`,
`maxdamage`, `repaircost`, `enchantable`, `unbreakable`, `glider`, `fireresistant`, `hidetooltip`,
`glint`, `rarity`, `model`, `itemmodel`, `tooltipstyle`, `type`, `color`, `skull`, `texture`, `potion`,
`trim`, `book`.

## Permissions

| Permission | Default | Effect |
|---|---|---|
| `itemsmith.admin.item` | op | use `/item` |

## Languages

Itemsmith ships in English and French. Set the language in `config.yml`:

```yaml
language: en
```

Use `fr` for French.

- `lang/messages_<language>.yml` holds every chat message. Edit it freely.
- `lang/<language>.yml` translates the menu and item texts. Add or override any entry to customise a
  label. Missing entries fall back to the original text.

To add a language, copy `lang/messages_en.yml` and `lang/en.yml` to `messages_<code>.yml` and
`<code>.yml`, translate them, and set `language: <code>`. Changing the language needs a restart.

Text input in game goes through a sign. All messages accept MiniMessage. `/item reload` rereads
`config.yml` and the language files.

## Developer API

Add Itemsmith as a `depend` or `softdepend`, then get the service:

```java
ItemsmithApi.get().ifPresent(itemsmith -> itemsmith.openEditor(player));
```

`ItemsmithApi` opens the editor, undoes the last edit of a player and tells how many edits can be
undone.

`ItemEditEvent` fires before an edit replaces the item in hand, from the menu or the command. It
carries copies of the item before and after, and a short description of the change. Cancelling it
keeps the original item and leaves the undo history untouched.

## Updates and metrics

On start Itemsmith checks the latest GitHub release and tells the console and players with
`itemsmith.admin.item` when a newer version exists. Set `update-checker: false` in `config.yml` to
turn it off. Anonymous usage statistics go through bStats and follow the global bStats opt-out in
`plugins/bStats/config.yml`.

## Building

```bash
mvn package
```

The plugin is in `target/Itemsmith.jar`. Java 21 is required.
