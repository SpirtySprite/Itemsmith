package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.command.CommandBase;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.scheduler.Scheduling;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.text.Tr;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ItemEditCommand extends CommandBase {

    private static final String PERMISSION = "itemsmith.admin.item";
    private static final List<String> EDIT_ALIASES = List.of("edit", "editer", "modifier");
    private static final List<String> FIELDS = List.of("name", "lore", "enchant", "flag", "attribute", "amount",
            "maxstack", "damage", "maxdamage", "unbreakable", "glint", "glider", "fireresistant", "hidetooltip",
            "rarity", "model", "itemmodel", "tooltipstyle", "enchantable", "type", "color", "skull", "texture",
            "potion", "trim", "book", "repaircost");
    private static final List<String> ROOT = merge(List.of("edit", "undo", "redo", "info", "help", "reload", "save",
            "load", "library", "delete", "export", "give"), FIELDS);
    private static final List<String> LIBRARY_ACTIONS = List.of("load", "delete", "give");
    private static final List<String> STATES = List.of("on", "off");
    private static final List<String> RESET = List.of("reset");
    private static final int MATERIAL_SUGGESTIONS = 60;

    private final ItemEditService service;
    private final ItemEditMenu menu;
    private Runnable reloadSettings = () -> {
    };

    public void onReload(Runnable action) {
        this.reloadSettings = action;
    }

    private final ItemLibrary library;
    private final ItemLibraryMenu libraryMenu;

    public ItemEditCommand(ItemEditService service, ItemEditMenu menu, ItemLibrary library) {
        super(PERMISSION, false);
        this.service = service;
        this.menu = menu;
        this.library = library;
        this.libraryMenu = new ItemLibraryMenu(library);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        String[] rest = strip(args);
        if (rest.length > 0 && List.of("give", "donner").contains(ItemLookup.normalize(rest[0]))) {
            give(sender, rest);
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "general.players-only");
            return;
        }
        if (rest.length == 0) {
            menu.open(player);
            return;
        }
        switch (ItemLookup.normalize(rest[0])) {
            case "undo", "annuler" -> service.undo(player);
            case "redo", "refaire" -> service.redo(player);
            case "save", "sauver" -> save(player, rest);
            case "load", "charger" -> load(player, rest);
            case "library", "bibliotheque" -> libraryMenu.open(player);
            case "delete", "supprimer" -> delete(player, rest);
            case "export", "exporter" -> export(player);
            case "info" -> info(player);
            case "help", "aide" -> Messages.lines("item-edit.usage").forEach(player::sendMessage);
            case "reload", "recharger" -> {
                reloadSettings.run();
                Guis.success(player);
                Messages.send(player, "item-edit.reloaded");
            }
            default -> {
                try {
                    edit(player, ItemLookup.normalize(rest[0]), rest);
                } catch (EditException invalid) {
                    service.fail(player, invalid.getMessage());
                }
            }
        }
    }

    private void edit(Player player, String field, String[] args) {
        switch (field) {
            case "name", "nom" -> {
                need(args, 2, "/item edit name <texte|reset>");
                String text = ItemText.join(args, 1);
                apply(player, item -> args.length == 2 && ItemLookup.reset(args[1])
                        ? ItemEditor.resetName(item) : ItemEditor.name(item, text));
            }
            case "lore", "description" -> lore(player, args);
            case "enchant", "enchantement" -> enchant(player, args);
            case "flag", "masquer" -> {
                need(args, 2, "/item edit flag <masquage|all> [on|off]");
                String state = args.length > 2 ? args[2] : null;
                if (ItemLookup.normalize(args[1]).equals("all")) {
                    boolean hide = ItemLookup.state(state, false);
                    apply(player, item -> ItemEditor.allFlags(item, hide));
                } else {
                    ItemFlag flag = ItemLookup.flag(args[1]);
                    apply(player, item -> ItemEditor.flag(item, flag, state));
                }
            }
            case "attribute", "attribut" -> attribute(player, args);
            case "amount", "quantite" -> {
                need(args, 2, "/item edit amount <1-99>");
                int amount = ItemLookup.integer(args[1], 1, ItemLookup.MAX_STACK, Tr.t("La quantité"));
                apply(player, item -> ItemEditor.amount(item, amount));
            }
            case "maxstack" -> {
                need(args, 2, "/item edit maxstack <1-99|reset>");
                Integer size = ItemLookup.reset(args[1]) ? null
                        : ItemLookup.integer(args[1], 1, ItemLookup.MAX_STACK, Tr.t("La taille de pile"));
                apply(player, item -> ItemEditor.maxStack(item, size));
            }
            case "damage", "degats" -> {
                need(args, 2, "/item edit damage <nombre>");
                int damage = ItemLookup.integer(args[1], 0, Integer.MAX_VALUE, Tr.t("Les dégâts"));
                apply(player, item -> ItemEditor.damage(item, damage));
            }
            case "maxdamage", "durabilite" -> {
                need(args, 2, "/item edit maxdamage <nombre|reset>");
                Integer max = ItemLookup.reset(args[1]) ? null
                        : ItemLookup.integer(args[1], 1, Integer.MAX_VALUE, Tr.t("La durabilité maximale"));
                apply(player, item -> ItemEditor.maxDamage(item, max));
            }
            case "unbreakable", "incassable" -> apply(player, item -> ItemEditor.unbreakable(item, optional(args)));
            case "glider", "planeur" -> apply(player, item -> ItemEditor.glider(item, optional(args)));
            case "fireresistant", "ignifuge" -> apply(player, item -> ItemEditor.fireResistant(item, optional(args)));
            case "hidetooltip" -> apply(player, item -> ItemEditor.hideTooltip(item, optional(args)));
            case "glint", "brillance" -> {
                need(args, 2, "/item edit glint <on|off|reset>");
                Boolean glint = ItemLookup.reset(args[1]) ? null : ItemLookup.state(args[1], false);
                apply(player, item -> ItemEditor.glint(item, glint));
            }
            case "rarity", "rarete" -> {
                need(args, 2, "/item edit rarity <common|uncommon|rare|epic|reset>");
                var rarity = ItemLookup.reset(args[1]) ? null : ItemLookup.rarity(args[1]);
                apply(player, item -> ItemEditor.rarity(item, rarity));
            }
            case "model", "modele" -> {
                need(args, 2, "/item edit model <nombre|reset>");
                Integer model = ItemLookup.reset(args[1]) ? null
                        : ItemLookup.integer(args[1], Integer.MIN_VALUE, Integer.MAX_VALUE, Tr.t("Le modèle"));
                apply(player, item -> ItemEditor.customModelData(item, model));
            }
            case "itemmodel" -> {
                need(args, 2, "/item edit itemmodel <espace:nom|reset>");
                var key = ItemLookup.reset(args[1]) ? null : ItemLookup.key(args[1]);
                apply(player, item -> ItemEditor.itemModel(item, key));
            }
            case "tooltipstyle" -> {
                need(args, 2, "/item edit tooltipstyle <espace:nom|reset>");
                var key = ItemLookup.reset(args[1]) ? null : ItemLookup.key(args[1]);
                apply(player, item -> ItemEditor.tooltipStyle(item, key));
            }
            case "enchantable" -> {
                need(args, 2, "/item edit enchantable <nombre|reset>");
                Integer value = ItemLookup.reset(args[1]) ? null
                        : ItemLookup.integer(args[1], 1, Integer.MAX_VALUE, Tr.t("L'enchantabilité"));
                apply(player, item -> ItemEditor.enchantable(item, value));
            }
            case "type", "materiau" -> {
                need(args, 2, "/item edit type <matériau>");
                Material material = ItemLookup.material(args[1]);
                apply(player, item -> ItemEditor.type(item, material));
            }
            case "color", "couleur" -> {
                need(args, 2, "/item edit color <#RRGGBB|nom|reset>");
                var color = ItemLookup.reset(args[1]) ? null : ItemLookup.color(args[1]);
                apply(player, item -> ItemEditor.color(item, color));
            }
            case "skull", "tete" -> {
                need(args, 2, "/item edit skull <joueur>");
                if (service.holding(player)) {
                    service.skullOf(player, args[1], null);
                }
            }
            case "texture" -> {
                need(args, 2, "/item edit texture <valeur>");
                String texture = ItemText.join(args, 1);
                apply(player, item -> ItemEditor.skullTexture(item, texture));
            }
            case "potion" -> potion(player, args);
            case "trim", "garniture" -> {
                need(args, 2, "/item edit trim <matériau> <motif> | trim clear");
                if (ItemLookup.reset(args[1]) || ItemLookup.normalize(args[1]).equals("clear")) {
                    apply(player, item -> ItemEditor.trim(item, null, null));
                    return;
                }
                need(args, 3, "/item edit trim <matériau> <motif>");
                var material = ItemLookup.trimMaterial(args[1]);
                var pattern = ItemLookup.trimPattern(args[2]);
                apply(player, item -> ItemEditor.trim(item, material, pattern));
            }
            case "book", "livre" -> {
                need(args, 3, "/item edit book <title|author> <texte>");
                String text = ItemText.join(args, 2);
                switch (ItemLookup.normalize(args[1])) {
                    case "title", "titre" -> apply(player, item -> ItemEditor.bookTitle(item, text));
                    case "author", "auteur" -> apply(player, item -> ItemEditor.bookAuthor(item, text));
                    default -> throw new EditException(Tr.t("Utilisation : /item edit book <title|author> <texte>"));
                }
            }
            case "repaircost", "reparation" -> {
                need(args, 2, "/item edit repaircost <nombre>");
                int cost = ItemLookup.integer(args[1], 0, Integer.MAX_VALUE, Tr.t("Le coût de réparation"));
                apply(player, item -> ItemEditor.repairCost(item, cost));
            }
            default -> Messages.lines("item-edit.usage").forEach(player::sendMessage);
        }
    }

    private void lore(Player player, String[] args) {
        need(args, 2, "/item edit lore <add|set|insert|remove|move|clear>");
        switch (ItemLookup.normalize(args[1])) {
            case "add", "ajouter" -> {
                need(args, 3, "/item edit lore add <texte>");
                Component line = ItemText.parse(ItemText.join(args, 2));
                apply(player, item -> ItemEditor.lore(item, lore -> LoreEdit.add(lore, line), Tr.t("Ligne ajoutée")));
            }
            case "set", "modifier" -> {
                need(args, 4, "/item edit lore set <ligne> <texte>");
                int index = ItemLookup.integer(args[2], 1, LoreEdit.MAX_LINES, Tr.t("La ligne"));
                Component line = ItemText.parse(ItemText.join(args, 3));
                apply(player, item -> ItemEditor.lore(item, lore -> LoreEdit.set(lore, index, line),
                        Tr.t("Ligne ") + index + Tr.t(" modifiée")));
            }
            case "insert", "inserer" -> {
                need(args, 4, "/item edit lore insert <ligne> <texte>");
                int index = ItemLookup.integer(args[2], 1, LoreEdit.MAX_LINES, Tr.t("La ligne"));
                Component line = ItemText.parse(ItemText.join(args, 3));
                apply(player, item -> ItemEditor.lore(item, lore -> LoreEdit.insert(lore, index, line),
                        Tr.t("Ligne insérée en position ") + index));
            }
            case "remove", "retirer" -> {
                need(args, 3, "/item edit lore remove <ligne>");
                int index = ItemLookup.integer(args[2], 1, LoreEdit.MAX_LINES, Tr.t("La ligne"));
                apply(player, item -> ItemEditor.lore(item, lore -> LoreEdit.remove(lore, index),
                        Tr.t("Ligne ") + index + Tr.t(" retirée")));
            }
            case "move", "deplacer" -> {
                need(args, 4, "/item edit lore move <ligne> <position>");
                int from = ItemLookup.integer(args[2], 1, LoreEdit.MAX_LINES, Tr.t("La ligne"));
                int to = ItemLookup.integer(args[3], 1, LoreEdit.MAX_LINES, Tr.t("La position"));
                apply(player, item -> ItemEditor.lore(item, lore -> LoreEdit.move(lore, from, to),
                        Tr.t("Ligne ") + from + Tr.t(" déplacée en position ") + to));
            }
            case "clear", "effacer" -> apply(player, item -> ItemEditor.lore(item, lore -> List.of(),
                    Tr.t("Description effacée")));
            default -> throw new EditException(Tr.t("Utilisation : /item edit lore <add|set|insert|remove|move|clear>"));
        }
    }

    private void enchant(Player player, String[] args) {
        need(args, 2, "/item edit enchant <enchantement> [niveau] | remove | clear");
        switch (ItemLookup.normalize(args[1])) {
            case "remove", "retirer" -> {
                need(args, 3, "/item edit enchant remove <enchantement>");
                Enchantment enchantment = ItemLookup.enchantment(args[2]);
                apply(player, item -> ItemEditor.unenchant(item, enchantment));
            }
            case "clear", "effacer" -> apply(player, ItemEditor::clearEnchants);
            default -> {
                Enchantment enchantment = ItemLookup.enchantment(args[1]);
                int level = args.length > 2
                        ? ItemLookup.integer(args[2], 1, ItemLookup.MAX_ENCHANT_LEVEL, Tr.t("Le niveau")) : 1;
                apply(player, item -> ItemEditor.enchant(item, enchantment, level));
            }
        }
    }

    private void attribute(Player player, String[] args) {
        need(args, 2, "/item edit attribute <add|remove|clear>");
        switch (ItemLookup.normalize(args[1])) {
            case "add", "ajouter" -> {
                need(args, 4, "/item edit attribute add <attribut> <valeur> [add|percent|multiply] [emplacement]");
                var attribute = ItemLookup.attribute(args[2]);
                double amount = ItemLookup.decimal(args[3], Tr.t("La valeur"));
                AttributeModifier.Operation operation = ItemLookup.operation(args.length > 4 ? args[4] : null);
                var slot = ItemLookup.slot(args.length > 5 ? args[5] : null);
                apply(player, item -> ItemEditor.addAttribute(item, attribute, amount, operation, slot));
            }
            case "remove", "retirer" -> {
                need(args, 3, "/item edit attribute remove <attribut>");
                var attribute = ItemLookup.attribute(args[2]);
                apply(player, item -> ItemEditor.removeAttribute(item, attribute));
            }
            case "clear", "reset", "effacer" -> apply(player, ItemEditor::resetAttributes);
            default -> throw new EditException(Tr.t("Utilisation : /item edit attribute <add|remove|clear>"));
        }
    }

    private void potion(Player player, String[] args) {
        need(args, 2, "/item edit potion <add|remove|type|clear>");
        switch (ItemLookup.normalize(args[1])) {
            case "add", "ajouter" -> {
                need(args, 4, "/item edit potion add <effet> <secondes> [niveau]");
                var effect = ItemLookup.effect(args[2]);
                int seconds = ItemLookup.integer(args[3], 1, Integer.MAX_VALUE / 20, Tr.t("La durée"));
                int level = args.length > 4
                        ? ItemLookup.integer(args[4], 1, ItemLookup.MAX_ENCHANT_LEVEL, Tr.t("Le niveau")) : 1;
                apply(player, item -> ItemEditor.potionEffect(item, effect, seconds, level));
            }
            case "remove", "retirer" -> {
                need(args, 3, "/item edit potion remove <effet>");
                var effect = ItemLookup.effect(args[2]);
                apply(player, item -> ItemEditor.removePotionEffect(item, effect));
            }
            case "type" -> {
                need(args, 3, "/item edit potion type <potion|reset>");
                var type = ItemLookup.reset(args[2]) ? null : ItemLookup.potionType(args[2]);
                apply(player, item -> ItemEditor.potionType(item, type));
            }
            case "clear", "effacer" -> apply(player, ItemEditor::clearPotion);
            default -> throw new EditException(Tr.t("Utilisation : /item edit potion <add|remove|type|clear>"));
        }
    }

    private void save(Player player, String[] args) {
        if (!service.holding(player)) {
            return;
        }
        String id = args.length > 1 ? ItemLibrary.normalize(args[1]) : "";
        if (!ItemLibrary.validId(id)) {
            service.fail(player, Tr.t("Identifiant invalide, utilisez 1 à 32 caractères parmi a-z, 0-9, - et _"));
            return;
        }
        boolean replaced = library.get(id).isPresent();
        library.save(id, service.held(player));
        Guis.success(player);
        Messages.send(player, replaced ? "item-edit.library-replaced" : "item-edit.library-saved", Mini.value("id", id));
    }

    private void load(Player player, String[] args) {
        String id = args.length > 1 ? args[1] : "";
        library.get(id).ifPresentOrElse(item -> {
            ItemLibraryMenu.give(player, item);
            Guis.success(player);
            Messages.send(player, "item-edit.library-loaded", Mini.value("id", ItemLibrary.normalize(id)));
        }, () -> unknown(player, id));
    }

    private void delete(Player player, String[] args) {
        String id = args.length > 1 ? args[1] : "";
        if (library.delete(id)) {
            Guis.success(player);
            Messages.send(player, "item-edit.library-deleted", Mini.value("id", ItemLibrary.normalize(id)));
        } else {
            unknown(player, id);
        }
    }

    private void unknown(CommandSender sender, String id) {
        if (sender instanceof Player player) {
            Guis.deny(player);
        }
        Messages.send(sender, "item-edit.library-unknown", Mini.value("id", id));
    }

    private void export(Player player) {
        if (!service.holding(player)) {
            return;
        }
        String command = giveCommand(service.held(player));
        Component copy = Component.text(command).clickEvent(ClickEvent.copyToClipboard(command))
                .hoverEvent(HoverEvent.showText(Component.text(Tr.t("Cliquez pour copier"))));
        Messages.send(player, "item-edit.exported", Mini.component("command", copy));
    }

    static String giveCommand(ItemStack item) {
        String components = item.hasItemMeta() ? item.getItemMeta().getAsComponentString() : "";
        return "/give @p " + item.getType().getKey().asString() + (components == null ? "" : components)
                + (item.getAmount() > 1 ? " " + item.getAmount() : "");
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.send(sender, "item-edit.give-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.send(sender, "general.unknown-player", Mini.value("player", args[1]));
            return;
        }
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[3])));
            } catch (NumberFormatException invalid) {
                Messages.send(sender, "item-edit.give-usage");
                return;
            }
        }
        int count = amount;
        library.get(args[2]).ifPresentOrElse(item -> {
            Scheduling.entity(target, () -> {
                int left = count;
                while (left > 0) {
                    ItemStack stack = item.clone();
                    int size = Math.min(left, Math.max(1, stack.getMaxStackSize()));
                    stack.setAmount(size);
                    ItemLibraryMenu.give(target, stack);
                    left -= size;
                }
            });
            Messages.send(sender, "item-edit.given", Mini.value("id", ItemLibrary.normalize(args[2])),
                    Mini.value("player", target.getName()), Mini.value("amount", String.valueOf(count)));
        }, () -> unknown(sender, args[2]));
    }

    private void info(Player player) {
        if (!service.holding(player)) {
            return;
        }
        ItemStack item = service.held(player);
        player.sendMessage(Messages.get("item-edit.info-header",
                Mini.value("item", item.getType().getKey().getKey())));
        for (Map.Entry<String, String> line : describe(item, service.undoable(player.getUniqueId())).entrySet()) {
            player.sendMessage(Messages.get("item-edit.info-line",
                    Mini.value("label", line.getKey()), Mini.value("value", line.getValue())));
        }
    }

    static Map<String, String> describe(ItemStack item, int undoable) {
        Map<String, String> lines = new LinkedHashMap<>();
        ItemMeta meta = item.getItemMeta();
        lines.put(Tr.t("Quantité"), item.getAmount() + " / " + item.getMaxStackSize());
        if (meta == null) {
            return lines;
        }
        lines.put(Tr.t("Nom"), meta.hasCustomName() ? ItemText.plain(meta.customName()) : Tr.t("par défaut"));
        lines.put(Tr.t("Description"), meta.hasLore() ? meta.lore().size() + Tr.t(" lignes") : Tr.t("aucune"));
        StringBuilder enchants = new StringBuilder();
        meta.getEnchants().forEach((enchantment, level) -> enchants.append(enchants.isEmpty() ? "" : ", ")
                .append(ItemLookup.shortKey(enchantment)).append(' ').append(level));
        lines.put(Tr.t("Enchantements"), enchants.isEmpty() ? Tr.t("aucun") : enchants.toString());
        lines.put(Tr.t("Masquages"), meta.getItemFlags().isEmpty() ? Tr.t("aucun") : String.valueOf(meta.getItemFlags().size()));
        lines.put(Tr.t("Attributs"), meta.hasAttributeModifiers()
                ? String.valueOf(meta.getAttributeModifiers().size()) : Tr.t("par défaut"));
        if (meta instanceof Damageable damageable && (damageable.hasMaxDamage() || item.getType().getMaxDurability() > 0)) {
            int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
            lines.put(Tr.t("Durabilité"), Math.max(0, max - damageable.getDamage()) + " / " + max);
        }
        lines.put(Tr.t("Incassable"), meta.isUnbreakable() ? Tr.t("oui") : Tr.t("non"));
        lines.put(Tr.t("Brillance"), meta.hasEnchantmentGlintOverride()
                ? (meta.getEnchantmentGlintOverride() ? Tr.t("forcée") : Tr.t("retirée")) : Tr.t("par défaut"));
        lines.put(Tr.t("Rareté"), meta.hasRarity() ? ItemNaming.rarity(meta.getRarity()) : Tr.t("par défaut"));
        lines.put(Tr.t("Modèle personnalisé"), meta.hasCustomModelData() ? String.valueOf(meta.getCustomModelData()) : Tr.t("aucun"));
        lines.put(Tr.t("Modèle d'objet"), meta.hasItemModel() ? meta.getItemModel().toString() : Tr.t("par défaut"));
        lines.put(Tr.t("Style d'infobulle"), meta.hasTooltipStyle() ? meta.getTooltipStyle().toString() : Tr.t("par défaut"));
        lines.put(Tr.t("Planeur"), meta.isGlider() ? Tr.t("oui") : Tr.t("non"));
        lines.put(Tr.t("Résiste au feu"), meta.hasDamageResistant() ? Tr.t("oui") : Tr.t("non"));
        lines.put(Tr.t("Infobulle masquée"), meta.isHideTooltip() ? Tr.t("oui") : Tr.t("non"));
        lines.put(Tr.t("Annulations possibles"), String.valueOf(undoable));
        return lines;
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
    }

    private static void need(String[] args, int length, String usage) {
        if (args.length < length) {
            throw new EditException(Tr.t("Utilisation : ") + usage);
        }
    }

    private static String optional(String[] args) {
        return args.length > 1 ? args[1] : null;
    }

    private static String[] strip(String[] args) {
        if (args.length > 0 && EDIT_ALIASES.contains(ItemLookup.normalize(args[0]))) {
            return Arrays.copyOfRange(args, 1, args.length);
        }
        return args;
    }

    private static List<String> merge(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        boolean edit = args.length > 0 && EDIT_ALIASES.contains(ItemLookup.normalize(args[0]));
        String[] rest = edit ? Arrays.copyOfRange(args, 1, args.length) : args;
        if (rest.length == 0) {
            return List.of();
        }
        String current = rest[rest.length - 1];
        if (rest.length == 1) {
            return match(edit ? FIELDS : ROOT, current);
        }
        String action = ItemLookup.normalize(rest[0]);
        if (!edit && LIBRARY_ACTIONS.contains(action)) {
            if (action.equals("give") && rest.length == 2) {
                return match(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), current);
            }
            if (rest.length == (action.equals("give") ? 3 : 2)) {
                return match(library.ids(), current);
            }
            return List.of();
        }
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        return match(suggestions(player, action, rest), current);
    }

    private List<String> suggestions(Player player, String field, String[] rest) {
        int position = rest.length;
        String sub = ItemLookup.normalize(rest[1]);
        return switch (field) {
            case "name", "nom", "maxdamage", "durabilite", "model", "modele", "enchantable", "itemmodel",
                 "tooltipstyle" -> position == 2 ? RESET : List.of();
            case "lore", "description" -> position == 2
                    ? List.of("add", "set", "insert", "remove", "move", "clear")
                    : position <= (sub.equals("move") ? 4 : 3) && !sub.equals("add") && !sub.equals("clear")
                    ? loreLines(player) : List.of();
            case "enchant", "enchantement" -> {
                if (position == 2) {
                    yield merge(List.of("remove", "clear"), ItemLookup.keys(Registry.ENCHANTMENT));
                }
                if (position == 3 && sub.equals("remove")) {
                    yield heldEnchants(player);
                }
                yield position == 3 && !sub.equals("clear") ? List.of("1", "5", "10", "100", "255") : List.of();
            }
            case "flag", "masquer" -> position == 2 ? merge(List.of("all"), flagNames())
                    : position == 3 ? STATES : List.of();
            case "attribute", "attribut" -> switch (position) {
                case 2 -> List.of("add", "remove", "clear");
                case 3 -> sub.equals("clear") ? List.of() : ItemLookup.keys(Registry.ATTRIBUTE);
                case 4 -> sub.equals("add") ? List.of("1", "0.5", "-1") : List.of();
                case 5 -> sub.equals("add") ? List.of("add", "percent", "multiply") : List.of();
                case 6 -> sub.equals("add") ? ItemLookup.SLOT_GROUPS.stream().map(Object::toString).toList() : List.of();
                default -> List.of();
            };
            case "amount", "quantite", "maxstack" -> position == 2 ? List.of("1", "16", "64", "99") : List.of();
            case "damage", "degats", "repaircost", "reparation" -> position == 2 ? List.of("0") : List.of();
            case "unbreakable", "incassable", "glider", "planeur", "fireresistant", "ignifuge", "hidetooltip" ->
                    position == 2 ? STATES : List.of();
            case "glint", "brillance" -> position == 2 ? List.of("on", "off", "reset") : List.of();
            case "rarity", "rarete" -> position == 2 ? List.of("common", "uncommon", "rare", "epic", "reset") : List.of();
            case "type", "materiau" -> position == 2 ? materials(rest[1]) : List.of();
            case "color", "couleur" -> position == 2 ? merge(RESET, List.copyOf(NamedTextColor.NAMES.keys())) : List.of();
            case "skull", "tete" -> position == 2 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
            case "potion" -> switch (position) {
                case 2 -> List.of("add", "remove", "type", "clear");
                case 3 -> sub.equals("type") ? merge(RESET, ItemLookup.keys(Registry.POTION))
                        : sub.equals("add") || sub.equals("remove") ? ItemLookup.keys(Registry.MOB_EFFECT) : List.of();
                case 4 -> sub.equals("add") ? List.of("30", "60", "300", "3600") : List.of();
                case 5 -> sub.equals("add") ? List.of("1", "2", "5", "10") : List.of();
                default -> List.of();
            };
            case "trim", "garniture" -> position == 2 ? merge(List.of("clear"), ItemLookup.keys(Registry.TRIM_MATERIAL))
                    : position == 3 && !sub.equals("clear") ? ItemLookup.keys(Registry.TRIM_PATTERN) : List.of();
            case "book", "livre" -> position == 2 ? List.of("title", "author") : List.of();
            default -> List.of();
        };
    }

    private List<String> loreLines(Player player) {
        ItemStack item = player == null ? null : service.held(player);
        ItemMeta meta = item == null ? null : item.getItemMeta();
        int size = meta != null && meta.hasLore() ? meta.lore().size() : 0;
        List<String> lines = new ArrayList<>();
        for (int line = 1; line <= Math.max(1, size + 1); line++) {
            lines.add(String.valueOf(line));
        }
        return lines;
    }

    private List<String> heldEnchants(Player player) {
        ItemStack item = player == null ? null : service.held(player);
        if (item == null || item.getItemMeta() == null) {
            return List.of();
        }
        return item.getItemMeta().getEnchants().keySet().stream().map(ItemLookup::shortKey).sorted().toList();
    }

    private static List<String> flagNames() {
        return Arrays.stream(ItemFlag.values())
                .map(flag -> flag.name().substring("HIDE_".length()).toLowerCase(Locale.ROOT))
                .toList();
    }

    private static List<String> materials(String prefix) {
        String typed = ItemLookup.normalize(prefix).replace("minecraft:", "");
        List<String> matches = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir() && !material.isLegacy()) {
                String key = material.getKey().getKey();
                if (key.startsWith(typed)) {
                    matches.add(key);
                    if (matches.size() >= MATERIAL_SUGGESTIONS) {
                        break;
                    }
                }
            }
        }
        return matches;
    }
}
