# Itemsmith

Éditeur d'objets en jeu pour Paper et Folia 1.21. Tenez un objet, tapez `/item` et modifiez tout
depuis un menu : nom, description, enchantements, attributs, drapeaux de masquage, modèle, style
d'infobulle, rareté, couleur, tête de joueur, durabilité et plus. Chaque modification peut être
annulée.

## Installation

1. Placez `Itemsmith.jar` dans `plugins/`.
2. Démarrez le serveur. `messages.yml` est créé dans `plugins/Itemsmith/`.

PlaceholderAPI est optionnel.

## Commandes

| Commande | Effet |
|---|---|
| `/item` | ouvre le menu d'édition de l'objet en main |
| `/item edit <champ> ...` | modifie un champ directement (voir `/item help`) |
| `/item undo` | annule la dernière modification |
| `/item info` | résume l'objet en main |

Alias : `/itemedit`, `/ie`, `/itemeditor`, `/edititem`.

Champs de `/item edit` : `name`, `lore`, `enchant`, `flag`, `attribute`, `amount`, `maxstack`,
`damage`, `maxdamage`, `repaircost`, `enchantable`, `unbreakable`, `glider`, `fireresistant`,
`hidetooltip`, `glint`, `rarity`, `model`, `itemmodel`, `tooltipstyle`, `type`, `color`, `skull`,
`texture`.

## Permissions

| Permission | Par défaut | Effet |
|---|---|---|
| `itemsmith.admin.item` | op | utiliser `/item` |

## Textes

Tous les messages sont dans `messages.yml` et acceptent MiniMessage. Les saisies de texte en jeu
passent par un panneau.

## Compilation

```bash
mvn package
```

Le plugin se trouve dans `target/Itemsmith.jar`. Java 21 est requis.
