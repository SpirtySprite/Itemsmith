package com.kirugoldzzzz.itemsmith;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.gui.Gui;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.item.ItemNames;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ItemEditMenu {

    private static final int ROWS = 6;

    private final ItemEditService service;
    private final ItemLoreMenu lore;
    private final ItemEnchantMenu enchants;
    private final ItemFlagMenu flags;
    private final ItemAttributeMenu attributes;
    private final ItemPotionMenu potions;

    public ItemEditMenu(ItemEditService service) {
        this.service = service;
        this.lore = new ItemLoreMenu(service, this::open);
        this.enchants = new ItemEnchantMenu(service, this::open);
        this.flags = new ItemFlagMenu(service, this::open);
        this.attributes = new ItemAttributeMenu(service, this::open);
        this.potions = new ItemPotionMenu(service, this::open);
    }

    public void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        long started = System.nanoTime();
        ItemStack item = service.held(player);
        ItemMeta meta = item.getItemMeta();
        Gui gui = Gui.builder()
                .rows(ROWS)
                .title(ItemStyle.guiTitle(Mini.plain(ItemNames.of(item))))
                .create();
        Guis.fill(gui);
        gui.setItem(1, 5, ItemBuilder.of(item).asGuiItem(event -> event.setCancelled(true)));
        if (meta != null) {
            identity(gui, player, item, meta);
            toggles(gui, player, item, meta);
            appearance(gui, player, item, meta);
            specifics(gui, player, meta);
        }
        gui.setItem(ROWS, 4, undoButton(player));
        gui.setItem(ROWS, 6, helpButton());
        gui.setItem(ROWS, 9, Guis.closeButton());
        gui.open(player);
        Guis.opened(started);
    }

    private void identity(Gui gui, Player player, ItemStack item, ItemMeta meta) {
        gui.setItem(2, 2, ItemStyle.button(Material.NAME_TAG, "Nom", ItemStyle.card("Nom affiché")
                .section("Actuel")
                .line(meta.hasCustomName() ? ItemText.plain(meta.customName()) : "Nom par défaut")
                .line(Palette.MUTED + "Texte long : /item edit name")
                .blank()
                .click("Clic gauche", "pour renommer")
                .click("Clic droit", "pour réinitialiser"), meta.hasCustomName(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, ItemEditor::resetName);
                return;
            }
            ItemPrompts.edit(service, viewer, "Nouveau nom", true,
                    text -> held -> ItemEditor.name(held, text), () -> open(viewer));
        }));

        int lines = meta.hasLore() ? meta.lore().size() : 0;
        gui.setItem(2, 3, ItemStyle.button(Material.WRITABLE_BOOK, "Description", ItemStyle.card("Lore")
                .blank()
                .count(Card.AMOUNT, "Lignes", lines)
                .blank()
                .click("pour gérer les lignes"), lines > 0, event -> lore.open(viewer(event.getWhoClicked()))));

        int enchantCount = meta.getEnchants().size();
        gui.setItem(2, 4, ItemStyle.button(Material.ENCHANTED_BOOK, "Enchantements", ItemStyle.card("Enchantements")
                .blank()
                .count(Card.STAR, "Appliqués", enchantCount)
                .line("Niveaux jusqu'à " + ItemLookup.MAX_ENCHANT_LEVEL + ", sans restriction")
                .blank()
                .click("pour gérer les enchantements"), enchantCount > 0,
                event -> enchants.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 5, ItemStyle.button(Material.GOLDEN_SWORD, "Attributs", ItemStyle.card("Attributs")
                .blank()
                .stat(Card.FLAG, "Modificateurs", meta.hasAttributeModifiers()
                        ? String.valueOf(meta.getAttributeModifiers().size()) : "par défaut")
                .blank()
                .click("pour gérer les attributs"), meta.hasAttributeModifiers(),
                event -> attributes.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 6, ItemStyle.button(Material.SPYGLASS, "Masquages", ItemStyle.card("Infobulle")
                .blank()
                .stat(Card.FLAG, "Infos masquées", meta.getItemFlags().size() + " / "
                        + ItemFlag.values().length)
                .blank()
                .click("pour choisir quoi masquer"), !meta.getItemFlags().isEmpty(),
                event -> flags.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 7, ItemStyle.button(Material.BUNDLE, "Quantité", ItemStyle.card("Pile")
                .blank()
                .count(Card.AMOUNT, "Quantité", item.getAmount())
                .blank()
                .click("pour choisir la quantité"), false, event -> {
            Player viewer = viewer(event.getWhoClicked());
            ItemPrompts.edit(service, viewer, "Quantité 1 à 99", false, text -> {
                int amount = ItemLookup.integer(text, 1, ItemLookup.MAX_STACK, "La quantité");
                return held -> ItemEditor.amount(held, amount);
            }, () -> open(viewer));
        }));

        gui.setItem(2, 8, ItemStyle.button(Material.SHULKER_BOX, "Pile maximale", ItemStyle.card("Pile")
                .blank()
                .stat(Card.AMOUNT, "Taille maximale", item.getMaxStackSize() + (meta.hasMaxStackSize() ? "" : " (défaut)"))
                .blank()
                .click("Clic gauche", "pour la changer")
                .click("Clic droit", "pour la réinitialiser"), meta.hasMaxStackSize(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.maxStack(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, "Taille 1 à 99", false, text -> {
                int size = ItemLookup.integer(text, 1, ItemLookup.MAX_STACK, "La taille de pile");
                return held -> ItemEditor.maxStack(held, size);
            }, () -> open(viewer));
        }));
    }

    private void toggles(Gui gui, Player player, ItemStack item, ItemMeta meta) {
        gui.setItem(3, 2, toggle(Material.BEDROCK, "Incassable", meta.isUnbreakable(),
                "L'objet ne s'use jamais", ItemEditor::unbreakable));
        gui.setItem(3, 4, toggle(Material.ELYTRA, "Planeur", meta.isGlider(),
                "Permet de planer comme des élytres", ItemEditor::glider));
        gui.setItem(3, 5, toggle(Material.MAGMA_CREAM, "Résiste au feu", meta.hasDamageResistant(),
                "Ne brûle pas dans la lave ni le feu", ItemEditor::fireResistant));
        gui.setItem(3, 6, toggle(Material.GLASS, "Infobulle masquée", meta.isHideTooltip(),
                "Plus aucune infobulle au survol", ItemEditor::hideTooltip));

        Boolean glint = meta.hasEnchantmentGlintOverride() ? meta.getEnchantmentGlintOverride() : null;
        gui.setItem(3, 3, ItemStyle.button(Material.EXPERIENCE_BOTTLE, "Brillance", ItemStyle.card("Reflet")
                .blank()
                .stat(Card.STAR, "Actuelle", glint == null ? "par défaut" : glint ? "forcée" : "retirée")
                .blank()
                .click("pour passer à l'état suivant"), Boolean.TRUE.equals(glint), event -> {
            Boolean next = glint == null ? Boolean.TRUE : glint ? Boolean.FALSE : null;
            apply(viewer(event.getWhoClicked()), held -> ItemEditor.glint(held, next));
        }));

        ItemRarity rarity = meta.hasRarity() ? meta.getRarity() : null;
        gui.setItem(3, 7, ItemStyle.button(Material.AMETHYST_SHARD, "Rareté", ItemStyle.card("Couleur du nom")
                .blank()
                .stat(Card.STAR, "Actuelle", ItemNaming.rarity(rarity))
                .blank()
                .click("Clic gauche", "pour la rareté suivante")
                .click("Clic droit", "pour réinitialiser"), rarity != null, event -> {
            ItemRarity next = event.isRightClick() ? null : nextRarity(rarity);
            apply(viewer(event.getWhoClicked()), held -> ItemEditor.rarity(held, next));
        }));

        if (meta instanceof Damageable damageable) {
            int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
            gui.setItem(3, 8, ItemStyle.button(Material.DAMAGED_ANVIL, "Durabilité", ItemStyle.card("Usure")
                    .blank()
                    .stat(Card.FLAG, "Dégâts", damageable.getDamage())
                    .stat(Card.FLAG, "Durabilité maximale", max > 0 ? max + (damageable.hasMaxDamage() ? "" : " (défaut)") : "aucune")
                    .blank()
                    .click("Clic gauche", "pour régler les dégâts")
                    .click("Clic droit", "pour régler le maximum")
                    .click("Maj + clic droit", "pour réinitialiser le maximum"), damageable.hasMaxDamage(), event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    apply(viewer, held -> ItemEditor.maxDamage(held, null));
                } else if (event.isRightClick()) {
                    ItemPrompts.edit(service, viewer, "Durabilité max", false, text -> {
                        int value = ItemLookup.integer(text, 1, Integer.MAX_VALUE, "La durabilité maximale");
                        return held -> ItemEditor.maxDamage(held, value);
                    }, () -> open(viewer));
                } else {
                    ItemPrompts.edit(service, viewer, "Dégâts", false, text -> {
                        int value = ItemLookup.integer(text, 0, Integer.MAX_VALUE, "Les dégâts");
                        return held -> ItemEditor.damage(held, value);
                    }, () -> open(viewer));
                }
            }));
        } else {
            gui.setItem(3, 8, ItemStyle.unavailable(Material.DAMAGED_ANVIL, "Durabilité", "Cet objet n'a pas de durabilité"));
        }
    }

    private void appearance(Gui gui, Player player, ItemStack item, ItemMeta meta) {
        gui.setItem(4, 2, ItemStyle.button(Material.ITEM_FRAME, "Modèle personnalisé", ItemStyle.card("Pack de ressources")
                .blank()
                .stat(Card.FLAG, "Valeur", meta.hasCustomModelData() ? meta.getCustomModelData() : "aucune")
                .blank()
                .click("Clic gauche", "pour choisir un nombre")
                .click("Clic droit", "pour retirer"), meta.hasCustomModelData(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.customModelData(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, "Modèle", false, text -> {
                int value = ItemLookup.integer(text, Integer.MIN_VALUE, Integer.MAX_VALUE, "Le modèle");
                return held -> ItemEditor.customModelData(held, value);
            }, () -> open(viewer));
        }));

        gui.setItem(4, 3, keyButton(Material.ARMOR_STAND, "Modèle d'objet", "Modèle 3D",
                meta.hasItemModel() ? meta.getItemModel().toString() : null,
                text -> held -> ItemEditor.itemModel(held, ItemLookup.key(text)),
                held -> ItemEditor.itemModel(held, null)));
        gui.setItem(4, 4, keyButton(Material.PAPER, "Style d'infobulle", "Cadre d'infobulle",
                meta.hasTooltipStyle() ? meta.getTooltipStyle().toString() : null,
                text -> held -> ItemEditor.tooltipStyle(held, ItemLookup.key(text)),
                held -> ItemEditor.tooltipStyle(held, null)));

        gui.setItem(4, 5, ItemStyle.button(Material.CRAFTING_TABLE, "Matériau", ItemStyle.card("Type d'objet")
                .blank()
                .stat(Card.FLAG, "Actuel", item.getType().getKey().getKey())
                .line("Le nom, la description et les données restent")
                .blank()
                .click("pour changer de matériau"), false, event -> {
            Player viewer = viewer(event.getWhoClicked());
            ItemPrompts.edit(service, viewer, "Matériau", false, text -> {
                Material material = ItemLookup.material(text);
                return held -> ItemEditor.type(held, material);
            }, () -> open(viewer));
        }));

        gui.setItem(4, 6, ItemStyle.button(Material.LAPIS_LAZULI, "Enchantabilité", ItemStyle.card("Table d'enchantement")
                .blank()
                .stat(Card.STAR, "Valeur", meta.hasEnchantable() ? meta.getEnchantable() : "par défaut")
                .blank()
                .click("Clic gauche", "pour choisir la valeur")
                .click("Clic droit", "pour réinitialiser"), meta.hasEnchantable(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.enchantable(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, "Enchantabilité", false, text -> {
                int value = ItemLookup.integer(text, 1, Integer.MAX_VALUE, "L'enchantabilité");
                return held -> ItemEditor.enchantable(held, value);
            }, () -> open(viewer));
        }));

        if (meta instanceof Repairable repairable) {
            gui.setItem(4, 7, ItemStyle.button(Material.ANVIL, "Coût de réparation", ItemStyle.card("Enclume")
                    .blank()
                    .stat(Card.FLAG, "Niveaux", repairable.hasRepairCost() ? repairable.getRepairCost() : 0)
                    .blank()
                    .click("pour choisir le coût"), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                ItemPrompts.edit(service, viewer, "Coût", false, text -> {
                    int value = ItemLookup.integer(text, 0, Integer.MAX_VALUE, "Le coût de réparation");
                    return held -> ItemEditor.repairCost(held, value);
                }, () -> open(viewer));
            }));
        } else {
            gui.setItem(4, 7, ItemStyle.unavailable(Material.ANVIL, "Coût de réparation", "Cet objet ne passe pas à l'enclume"));
        }

        Card summary = ItemStyle.card("Résumé").section("Objet");
        for (Map.Entry<String, String> line : ItemEditCommand.describe(item, service.undoable(player.getUniqueId())).entrySet()) {
            summary.stat(Card.CATEGORY, line.getKey(), line.getValue());
        }
        gui.setItem(4, 8, Guis.display(Material.KNOWLEDGE_BOOK, ItemStyle.heading("Résumé"), summary.build()));
    }

    private void specifics(Gui gui, Player player, ItemMeta meta) {
        if (meta instanceof LeatherArmorMeta || meta instanceof PotionMeta) {
            gui.setItem(5, 3, ItemStyle.button(Material.RED_DYE, "Couleur", ItemStyle.card("Teinture")
                    .blank()
                    .line("Couleur du cuir ou de la potion")
                    .blank()
                    .click("Clic gauche", "pour choisir #RRGGBB ou un nom")
                    .click("Clic droit", "pour réinitialiser"), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.isRightClick()) {
                    apply(viewer, held -> ItemEditor.color(held, null));
                    return;
                }
                ItemPrompts.edit(service, viewer, "Couleur", false, text -> {
                    var color = ItemLookup.color(text);
                    return held -> ItemEditor.color(held, color);
                }, () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 3, ItemStyle.unavailable(Material.RED_DYE, "Couleur", "Seuls le cuir et les potions se teignent"));
        }

        if (meta instanceof SkullMeta) {
            gui.setItem(5, 4, ItemStyle.button(Material.PLAYER_HEAD, "Tête", ItemStyle.card("Skin")
                    .blank()
                    .line("Applique le skin d'un joueur")
                    .line("Texture brute : /item edit texture")
                    .blank()
                    .click("pour saisir un pseudo"), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                ItemPrompts.ask(viewer, "Pseudo", false, name -> service.skullOf(viewer, name, () -> open(viewer)),
                        () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 4, ItemStyle.unavailable(Material.PLAYER_HEAD, "Tête", "Réservé aux têtes de joueur"));
        }

        if (meta instanceof PotionMeta potion) {
            gui.setItem(5, 5, ItemStyle.button(Material.POTION, "Effets", ItemStyle.card("Potion")
                    .blank()
                    .count(Card.STAR, "Effets personnalisés", potion.getCustomEffects().size())
                    .blank()
                    .click("pour gérer les effets"), potion.hasCustomEffects(),
                    event -> potions.open(viewer(event.getWhoClicked()))));
        } else {
            gui.setItem(5, 5, ItemStyle.unavailable(Material.POTION, "Effets", "Réservé aux potions et flèches à effet"));
        }

        if (meta instanceof ArmorMeta armor) {
            gui.setItem(5, 6, ItemStyle.button(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Garniture", ItemStyle.card("Armure")
                    .blank()
                    .stat(Card.STAR, "Actuelle", armor.hasTrim()
                            ? ItemLookup.shortKey(armor.getTrim().getPattern()) + " en " + ItemLookup.shortKey(armor.getTrim().getMaterial())
                            : "aucune")
                    .blank()
                    .click("Clic gauche", "pour choisir une garniture")
                    .click("Clic droit", "pour la retirer"), armor.hasTrim(), event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.isRightClick()) {
                    apply(viewer, held -> ItemEditor.trim(held, null, null));
                    return;
                }
                pickTrim(viewer);
            }));
        } else {
            gui.setItem(5, 6, ItemStyle.unavailable(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Garniture", "Réservé aux pièces d'armure"));
        }

        if (meta instanceof BookMeta) {
            gui.setItem(5, 7, ItemStyle.button(Material.WRITTEN_BOOK, "Livre", ItemStyle.card("Livre écrit")
                    .blank()
                    .click("Clic gauche", "pour changer le titre")
                    .click("Clic droit", "pour changer l'auteur"), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                boolean author = event.isRightClick();
                ItemPrompts.edit(service, viewer, author ? "Auteur" : "Titre", true,
                        text -> held -> author ? ItemEditor.bookAuthor(held, text) : ItemEditor.bookTitle(held, text),
                        () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 7, ItemStyle.unavailable(Material.WRITTEN_BOOK, "Livre", "Réservé aux livres écrits"));
        }
    }

    private void pickTrim(Player player) {
        ItemPickerMenu.open(player, "Matériau de garniture", ItemLookup.sorted(Registry.TRIM_MATERIAL),
                material -> ItemPickerMenu.icon(ItemNaming.trimMaterialIcon(material),
                        Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.trimMaterial(material)),
                        ItemStyle.card("Étape 1 sur 2").blank().click("pour choisir ce matériau").build()),
                (viewer, material) -> ItemPickerMenu.open(viewer, "Motif de garniture",
                        ItemLookup.sorted(Registry.TRIM_PATTERN),
                        pattern -> ItemPickerMenu.icon(ItemNaming.trimPatternIcon(pattern),
                                Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.trimPattern(pattern)),
                                ItemStyle.card("Étape 2 sur 2").blank().click("pour appliquer ce motif").build()),
                        (chooser, pattern) -> {
                            service.apply(chooser, held -> ItemEditor.trim(held, material, pattern));
                            open(chooser);
                        }, () -> pickTrim(viewer)),
                () -> open(player));
    }

    private GuiItem toggle(Material material, String name, boolean active, String description,
                           BiFunction<ItemStack, String, Edit> change) {
        return ItemStyle.button(material, name, ItemStyle.card("Option")
                .section("Description")
                .line(description)
                .blank()
                .stat(Card.FLAG, "État", ItemStyle.state(active))
                .blank()
                .click(active ? "pour désactiver" : "pour activer"), active,
                event -> apply(viewer(event.getWhoClicked()), held -> change.apply(held, active ? "off" : "on")));
    }

    private GuiItem keyButton(Material material, String name, String tag, String current,
                              Function<String, Function<ItemStack, Edit>> set, Function<ItemStack, Edit> reset) {
        return ItemStyle.button(material, name, ItemStyle.card(tag)
                .blank()
                .stat(Card.FLAG, "Actuel", current == null ? "par défaut" : current)
                .blank()
                .click("Clic gauche", "pour saisir espace:nom")
                .click("Clic droit", "pour réinitialiser"), current != null, event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, reset);
                return;
            }
            ItemPrompts.edit(service, viewer, "espace:nom", true, set, () -> open(viewer));
        });
    }

    private GuiItem undoButton(Player player) {
        int undoable = service.undoable(player.getUniqueId());
        return Guis.item(Material.RECOVERY_COMPASS, ItemStyle.heading("Annuler"), ItemStyle.card("Historique")
                .blank()
                .count(Card.TIME, "Modifications annulables", undoable)
                .blank()
                .click("pour annuler la dernière modification")
                .build(), undoable > 0, event -> {
            Player viewer = viewer(event.getWhoClicked());
            service.undo(viewer);
            open(viewer);
        });
    }

    private GuiItem helpButton() {
        return Guis.item(Material.BOOK, ItemStyle.heading("Commandes"), ItemStyle.card("Aide")
                .section("Description")
                .line("Tout se fait aussi en commande :")
                .line(Palette.WARNING + "/item edit <champ> ...")
                .blank()
                .click("pour afficher la liste dans le chat")
                .build(), false, event -> {
            Player viewer = viewer(event.getWhoClicked());
            viewer.closeInventory();
            Messages.lines("item-edit.usage").forEach(viewer::sendMessage);
        });
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }

    static ItemRarity nextRarity(ItemRarity current) {
        if (current == null) {
            return ItemRarity.COMMON;
        }
        ItemRarity[] values = ItemRarity.values();
        return current.ordinal() + 1 < values.length ? values[current.ordinal() + 1] : null;
    }

    private static Player viewer(HumanEntity entity) {
        return (Player) entity;
    }
}
