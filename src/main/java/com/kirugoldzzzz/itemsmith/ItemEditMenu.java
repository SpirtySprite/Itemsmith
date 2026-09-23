package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

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
        gui.setItem(2, 2, ItemStyle.button(Material.NAME_TAG, Tr.t("Nom"), ItemStyle.card(Tr.t("Nom affiché"))
                .section(Tr.t("Actuel"))
                .line(meta.hasCustomName() ? ItemText.plain(meta.customName()) : Tr.t("Nom par défaut"))
                .line(Palette.MUTED + Tr.t("Texte long : /item edit name"))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour renommer"))
                .click(Tr.t("Clic droit"), Tr.t("pour réinitialiser")), meta.hasCustomName(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, ItemEditor::resetName);
                return;
            }
            ItemPrompts.edit(service, viewer, Tr.t("Nouveau nom"), true,
                    text -> held -> ItemEditor.name(held, text), () -> open(viewer));
        }));

        int lines = meta.hasLore() ? meta.lore().size() : 0;
        gui.setItem(2, 3, ItemStyle.button(Material.WRITABLE_BOOK, Tr.t("Description"), ItemStyle.card(Tr.t("Lore"))
                .blank()
                .count(Card.AMOUNT, Tr.t("Lignes"), lines)
                .blank()
                .click(Tr.t("pour gérer les lignes")), lines > 0, event -> lore.open(viewer(event.getWhoClicked()))));

        int enchantCount = meta.getEnchants().size();
        gui.setItem(2, 4, ItemStyle.button(Material.ENCHANTED_BOOK, Tr.t("Enchantements"), ItemStyle.card(Tr.t("Enchantements"))
                .blank()
                .count(Card.STAR, Tr.t("Appliqués"), enchantCount)
                .line(Tr.t("Niveaux jusqu'à ") + ItemLookup.MAX_ENCHANT_LEVEL + Tr.t(", sans restriction"))
                .blank()
                .click(Tr.t("pour gérer les enchantements")), enchantCount > 0,
                event -> enchants.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 5, ItemStyle.button(Material.GOLDEN_SWORD, Tr.t("Attributs"), ItemStyle.card(Tr.t("Attributs"))
                .blank()
                .stat(Card.FLAG, Tr.t("Modificateurs"), meta.hasAttributeModifiers()
                        ? String.valueOf(meta.getAttributeModifiers().size()) : Tr.t("par défaut"))
                .blank()
                .click(Tr.t("pour gérer les attributs")), meta.hasAttributeModifiers(),
                event -> attributes.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 6, ItemStyle.button(Material.SPYGLASS, Tr.t("Masquages"), ItemStyle.card(Tr.t("Infobulle"))
                .blank()
                .stat(Card.FLAG, Tr.t("Infos masquées"), meta.getItemFlags().size() + " / "
                        + ItemFlag.values().length)
                .blank()
                .click(Tr.t("pour choisir quoi masquer")), !meta.getItemFlags().isEmpty(),
                event -> flags.open(viewer(event.getWhoClicked()))));

        gui.setItem(2, 7, ItemStyle.button(Material.BUNDLE, Tr.t("Quantité"), ItemStyle.card(Tr.t("Pile"))
                .blank()
                .count(Card.AMOUNT, Tr.t("Quantité"), item.getAmount())
                .blank()
                .click(Tr.t("pour choisir la quantité")), false, event -> {
            Player viewer = viewer(event.getWhoClicked());
            ItemPrompts.edit(service, viewer, Tr.t("Quantité 1 à 99"), false, text -> {
                int amount = ItemLookup.integer(text, 1, ItemLookup.MAX_STACK, Tr.t("La quantité"));
                return held -> ItemEditor.amount(held, amount);
            }, () -> open(viewer));
        }));

        gui.setItem(2, 8, ItemStyle.button(Material.SHULKER_BOX, Tr.t("Pile maximale"), ItemStyle.card(Tr.t("Pile"))
                .blank()
                .stat(Card.AMOUNT, Tr.t("Taille maximale"), item.getMaxStackSize() + (meta.hasMaxStackSize() ? "" : Tr.t(" (défaut)")))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour la changer"))
                .click(Tr.t("Clic droit"), Tr.t("pour la réinitialiser")), meta.hasMaxStackSize(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.maxStack(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, Tr.t("Taille 1 à 99"), false, text -> {
                int size = ItemLookup.integer(text, 1, ItemLookup.MAX_STACK, Tr.t("La taille de pile"));
                return held -> ItemEditor.maxStack(held, size);
            }, () -> open(viewer));
        }));
    }

    private void toggles(Gui gui, Player player, ItemStack item, ItemMeta meta) {
        gui.setItem(3, 2, toggle(Material.BEDROCK, Tr.t("Incassable"), meta.isUnbreakable(),
                Tr.t("L'objet ne s'use jamais"), ItemEditor::unbreakable));
        gui.setItem(3, 4, toggle(Material.ELYTRA, Tr.t("Planeur"), meta.isGlider(),
                Tr.t("Permet de planer comme des élytres"), ItemEditor::glider));
        gui.setItem(3, 5, toggle(Material.MAGMA_CREAM, Tr.t("Résiste au feu"), meta.hasDamageResistant(),
                Tr.t("Ne brûle pas dans la lave ni le feu"), ItemEditor::fireResistant));
        gui.setItem(3, 6, toggle(Material.GLASS, Tr.t("Infobulle masquée"), meta.isHideTooltip(),
                Tr.t("Plus aucune infobulle au survol"), ItemEditor::hideTooltip));

        Boolean glint = meta.hasEnchantmentGlintOverride() ? meta.getEnchantmentGlintOverride() : null;
        gui.setItem(3, 3, ItemStyle.button(Material.EXPERIENCE_BOTTLE, Tr.t("Brillance"), ItemStyle.card(Tr.t("Reflet"))
                .blank()
                .stat(Card.STAR, Tr.t("Actuelle"), glint == null ? Tr.t("par défaut") : glint ? Tr.t("forcée") : Tr.t("retirée"))
                .blank()
                .click(Tr.t("pour passer à l'état suivant")), Boolean.TRUE.equals(glint), event -> {
            Boolean next = glint == null ? Boolean.TRUE : glint ? Boolean.FALSE : null;
            apply(viewer(event.getWhoClicked()), held -> ItemEditor.glint(held, next));
        }));

        ItemRarity rarity = meta.hasRarity() ? meta.getRarity() : null;
        gui.setItem(3, 7, ItemStyle.button(Material.AMETHYST_SHARD, Tr.t("Rareté"), ItemStyle.card(Tr.t("Couleur du nom"))
                .blank()
                .stat(Card.STAR, Tr.t("Actuelle"), ItemNaming.rarity(rarity))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour la rareté suivante"))
                .click(Tr.t("Clic droit"), Tr.t("pour réinitialiser")), rarity != null, event -> {
            ItemRarity next = event.isRightClick() ? null : nextRarity(rarity);
            apply(viewer(event.getWhoClicked()), held -> ItemEditor.rarity(held, next));
        }));

        if (meta instanceof Damageable damageable) {
            int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
            gui.setItem(3, 8, ItemStyle.button(Material.DAMAGED_ANVIL, Tr.t("Durabilité"), ItemStyle.card(Tr.t("Usure"))
                    .blank()
                    .stat(Card.FLAG, Tr.t("Dégâts"), damageable.getDamage())
                    .stat(Card.FLAG, Tr.t("Durabilité maximale"), max > 0 ? max + (damageable.hasMaxDamage() ? "" : Tr.t(" (défaut)")) : Tr.t("aucune"))
                    .blank()
                    .click(Tr.t("Clic gauche"), Tr.t("pour régler les dégâts"))
                    .click(Tr.t("Clic droit"), Tr.t("pour régler le maximum"))
                    .click(Tr.t("Maj + clic droit"), Tr.t("pour réinitialiser le maximum")), damageable.hasMaxDamage(), event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    apply(viewer, held -> ItemEditor.maxDamage(held, null));
                } else if (event.isRightClick()) {
                    ItemPrompts.edit(service, viewer, Tr.t("Durabilité max"), false, text -> {
                        int value = ItemLookup.integer(text, 1, Integer.MAX_VALUE, Tr.t("La durabilité maximale"));
                        return held -> ItemEditor.maxDamage(held, value);
                    }, () -> open(viewer));
                } else {
                    ItemPrompts.edit(service, viewer, Tr.t("Dégâts"), false, text -> {
                        int value = ItemLookup.integer(text, 0, Integer.MAX_VALUE, Tr.t("Les dégâts"));
                        return held -> ItemEditor.damage(held, value);
                    }, () -> open(viewer));
                }
            }));
        } else {
            gui.setItem(3, 8, ItemStyle.unavailable(Material.DAMAGED_ANVIL, Tr.t("Durabilité"), Tr.t("Cet objet n'a pas de durabilité")));
        }
    }

    private void appearance(Gui gui, Player player, ItemStack item, ItemMeta meta) {
        gui.setItem(4, 2, ItemStyle.button(Material.ITEM_FRAME, Tr.t("Modèle personnalisé"), ItemStyle.card(Tr.t("Pack de ressources"))
                .blank()
                .stat(Card.FLAG, Tr.t("Valeur"), meta.hasCustomModelData() ? meta.getCustomModelData() : Tr.t("aucune"))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour choisir un nombre"))
                .click(Tr.t("Clic droit"), Tr.t("pour retirer")), meta.hasCustomModelData(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.customModelData(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, Tr.t("Modèle"), false, text -> {
                int value = ItemLookup.integer(text, Integer.MIN_VALUE, Integer.MAX_VALUE, Tr.t("Le modèle"));
                return held -> ItemEditor.customModelData(held, value);
            }, () -> open(viewer));
        }));

        gui.setItem(4, 3, keyButton(Material.ARMOR_STAND, Tr.t("Modèle d'objet"), Tr.t("Modèle 3D"),
                meta.hasItemModel() ? meta.getItemModel().toString() : null,
                text -> held -> ItemEditor.itemModel(held, ItemLookup.key(text)),
                held -> ItemEditor.itemModel(held, null)));
        gui.setItem(4, 4, keyButton(Material.PAPER, Tr.t("Style d'infobulle"), Tr.t("Cadre d'infobulle"),
                meta.hasTooltipStyle() ? meta.getTooltipStyle().toString() : null,
                text -> held -> ItemEditor.tooltipStyle(held, ItemLookup.key(text)),
                held -> ItemEditor.tooltipStyle(held, null)));

        gui.setItem(4, 5, ItemStyle.button(Material.CRAFTING_TABLE, Tr.t("Matériau"), ItemStyle.card(Tr.t("Type d'objet"))
                .blank()
                .stat(Card.FLAG, Tr.t("Actuel"), item.getType().getKey().getKey())
                .line(Tr.t("Le nom, la description et les données restent"))
                .blank()
                .click(Tr.t("pour changer de matériau")), false, event -> {
            Player viewer = viewer(event.getWhoClicked());
            ItemPrompts.edit(service, viewer, Tr.t("Matériau"), false, text -> {
                Material material = ItemLookup.material(text);
                return held -> ItemEditor.type(held, material);
            }, () -> open(viewer));
        }));

        gui.setItem(4, 6, ItemStyle.button(Material.LAPIS_LAZULI, Tr.t("Enchantabilité"), ItemStyle.card(Tr.t("Table d'enchantement"))
                .blank()
                .stat(Card.STAR, Tr.t("Valeur"), meta.hasEnchantable() ? meta.getEnchantable() : Tr.t("par défaut"))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour choisir la valeur"))
                .click(Tr.t("Clic droit"), Tr.t("pour réinitialiser")), meta.hasEnchantable(), event -> {
            Player viewer = viewer(event.getWhoClicked());
            if (event.isRightClick()) {
                apply(viewer, held -> ItemEditor.enchantable(held, null));
                return;
            }
            ItemPrompts.edit(service, viewer, Tr.t("Enchantabilité"), false, text -> {
                int value = ItemLookup.integer(text, 1, Integer.MAX_VALUE, Tr.t("L'enchantabilité"));
                return held -> ItemEditor.enchantable(held, value);
            }, () -> open(viewer));
        }));

        if (meta instanceof Repairable repairable) {
            gui.setItem(4, 7, ItemStyle.button(Material.ANVIL, Tr.t("Coût de réparation"), ItemStyle.card(Tr.t("Enclume"))
                    .blank()
                    .stat(Card.FLAG, Tr.t("Niveaux"), repairable.hasRepairCost() ? repairable.getRepairCost() : 0)
                    .blank()
                    .click(Tr.t("pour choisir le coût")), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                ItemPrompts.edit(service, viewer, Tr.t("Coût"), false, text -> {
                    int value = ItemLookup.integer(text, 0, Integer.MAX_VALUE, Tr.t("Le coût de réparation"));
                    return held -> ItemEditor.repairCost(held, value);
                }, () -> open(viewer));
            }));
        } else {
            gui.setItem(4, 7, ItemStyle.unavailable(Material.ANVIL, Tr.t("Coût de réparation"), Tr.t("Cet objet ne passe pas à l'enclume")));
        }

        Card summary = ItemStyle.card(Tr.t("Résumé")).section(Tr.t("Objet"));
        for (Map.Entry<String, String> line : ItemEditCommand.describe(item, service.undoable(player.getUniqueId())).entrySet()) {
            summary.stat(Card.CATEGORY, line.getKey(), line.getValue());
        }
        gui.setItem(4, 8, Guis.display(Material.KNOWLEDGE_BOOK, ItemStyle.heading(Tr.t("Résumé")), summary.build()));
    }

    private void specifics(Gui gui, Player player, ItemMeta meta) {
        if (meta instanceof LeatherArmorMeta || meta instanceof PotionMeta) {
            gui.setItem(5, 3, ItemStyle.button(Material.RED_DYE, Tr.t("Couleur"), ItemStyle.card(Tr.t("Teinture"))
                    .blank()
                    .line(Tr.t("Couleur du cuir ou de la potion"))
                    .blank()
                    .click(Tr.t("Clic gauche"), Tr.t("pour choisir #RRGGBB ou un nom"))
                    .click(Tr.t("Clic droit"), Tr.t("pour réinitialiser")), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.isRightClick()) {
                    apply(viewer, held -> ItemEditor.color(held, null));
                    return;
                }
                ItemPrompts.edit(service, viewer, Tr.t("Couleur"), false, text -> {
                    var color = ItemLookup.color(text);
                    return held -> ItemEditor.color(held, color);
                }, () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 3, ItemStyle.unavailable(Material.RED_DYE, Tr.t("Couleur"), Tr.t("Seuls le cuir et les potions se teignent")));
        }

        if (meta instanceof SkullMeta) {
            gui.setItem(5, 4, ItemStyle.button(Material.PLAYER_HEAD, Tr.t("Tête"), ItemStyle.card(Tr.t("Skin"))
                    .blank()
                    .line(Tr.t("Applique le skin d'un joueur"))
                    .line(Tr.t("Texture brute : /item edit texture"))
                    .blank()
                    .click(Tr.t("pour saisir un pseudo")), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                ItemPrompts.ask(viewer, Tr.t("Pseudo"), false, name -> service.skullOf(viewer, name, () -> open(viewer)),
                        () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 4, ItemStyle.unavailable(Material.PLAYER_HEAD, Tr.t("Tête"), Tr.t("Réservé aux têtes de joueur")));
        }

        if (meta instanceof PotionMeta potion) {
            gui.setItem(5, 5, ItemStyle.button(Material.POTION, Tr.t("Effets"), ItemStyle.card(Tr.t("Potion"))
                    .blank()
                    .count(Card.STAR, Tr.t("Effets personnalisés"), potion.getCustomEffects().size())
                    .blank()
                    .click(Tr.t("pour gérer les effets")), potion.hasCustomEffects(),
                    event -> potions.open(viewer(event.getWhoClicked()))));
        } else {
            gui.setItem(5, 5, ItemStyle.unavailable(Material.POTION, Tr.t("Effets"), Tr.t("Réservé aux potions et flèches à effet")));
        }

        if (meta instanceof ArmorMeta armor) {
            gui.setItem(5, 6, ItemStyle.button(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Tr.t("Garniture"), ItemStyle.card(Tr.t("Armure"))
                    .blank()
                    .stat(Card.STAR, Tr.t("Actuelle"), armor.hasTrim()
                            ? ItemLookup.shortKey(armor.getTrim().getPattern()) + Tr.t(" en ") + ItemLookup.shortKey(armor.getTrim().getMaterial())
                            : Tr.t("aucune"))
                    .blank()
                    .click(Tr.t("Clic gauche"), Tr.t("pour choisir une garniture"))
                    .click(Tr.t("Clic droit"), Tr.t("pour la retirer")), armor.hasTrim(), event -> {
                Player viewer = viewer(event.getWhoClicked());
                if (event.isRightClick()) {
                    apply(viewer, held -> ItemEditor.trim(held, null, null));
                    return;
                }
                pickTrim(viewer);
            }));
        } else {
            gui.setItem(5, 6, ItemStyle.unavailable(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Tr.t("Garniture"), Tr.t("Réservé aux pièces d'armure")));
        }

        if (meta instanceof BookMeta) {
            gui.setItem(5, 7, ItemStyle.button(Material.WRITTEN_BOOK, Tr.t("Livre"), ItemStyle.card(Tr.t("Livre écrit"))
                    .blank()
                    .click(Tr.t("Clic gauche"), Tr.t("pour changer le titre"))
                    .click(Tr.t("Clic droit"), Tr.t("pour changer l'auteur")), false, event -> {
                Player viewer = viewer(event.getWhoClicked());
                boolean author = event.isRightClick();
                ItemPrompts.edit(service, viewer, author ? Tr.t("Auteur") : Tr.t("Titre"), true,
                        text -> held -> author ? ItemEditor.bookAuthor(held, text) : ItemEditor.bookTitle(held, text),
                        () -> open(viewer));
            }));
        } else {
            gui.setItem(5, 7, ItemStyle.unavailable(Material.WRITTEN_BOOK, Tr.t("Livre"), Tr.t("Réservé aux livres écrits")));
        }
    }

    private void pickTrim(Player player) {
        ItemPickerMenu.open(player, Tr.t("Matériau de garniture"), ItemLookup.sorted(Registry.TRIM_MATERIAL),
                material -> ItemPickerMenu.icon(ItemNaming.trimMaterialIcon(material),
                        Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.trimMaterial(material)),
                        ItemStyle.card(Tr.t("Étape 1 sur 2")).blank().click(Tr.t("pour choisir ce matériau")).build()),
                (viewer, material) -> ItemPickerMenu.open(viewer, Tr.t("Motif de garniture"),
                        ItemLookup.sorted(Registry.TRIM_PATTERN),
                        pattern -> ItemPickerMenu.icon(ItemNaming.trimPatternIcon(pattern),
                                Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.trimPattern(pattern)),
                                ItemStyle.card(Tr.t("Étape 2 sur 2")).blank().click(Tr.t("pour appliquer ce motif")).build()),
                        (chooser, pattern) -> {
                            service.apply(chooser, held -> ItemEditor.trim(held, material, pattern));
                            open(chooser);
                        }, () -> pickTrim(viewer)),
                () -> open(player));
    }

    private GuiItem toggle(Material material, String name, boolean active, String description,
                           BiFunction<ItemStack, String, Edit> change) {
        return ItemStyle.button(material, name, ItemStyle.card(Tr.t("Option"))
                .section(Tr.t("Description"))
                .line(description)
                .blank()
                .stat(Card.FLAG, Tr.t("État"), ItemStyle.state(active))
                .blank()
                .click(active ? Tr.t("pour désactiver") : Tr.t("pour activer")), active,
                event -> apply(viewer(event.getWhoClicked()), held -> change.apply(held, active ? "off" : "on")));
    }

    private GuiItem keyButton(Material material, String name, String tag, String current,
                              Function<String, Function<ItemStack, Edit>> set, Function<ItemStack, Edit> reset) {
        return ItemStyle.button(material, name, ItemStyle.card(tag)
                .blank()
                .stat(Card.FLAG, Tr.t("Actuel"), current == null ? Tr.t("par défaut") : current)
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour saisir espace:nom"))
                .click(Tr.t("Clic droit"), Tr.t("pour réinitialiser")), current != null, event -> {
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
        return Guis.item(Material.RECOVERY_COMPASS, ItemStyle.heading(Tr.t("Annuler")), ItemStyle.card(Tr.t("Historique"))
                .blank()
                .count(Card.TIME, Tr.t("Modifications annulables"), undoable)
                .blank()
                .click(Tr.t("pour annuler la dernière modification"))
                .build(), undoable > 0, event -> {
            Player viewer = viewer(event.getWhoClicked());
            service.undo(viewer);
            open(viewer);
        });
    }

    private GuiItem helpButton() {
        return Guis.item(Material.BOOK, ItemStyle.heading(Tr.t("Commandes")), ItemStyle.card(Tr.t("Aide"))
                .section(Tr.t("Description"))
                .line(Tr.t("Tout se fait aussi en commande :"))
                .line(Palette.WARNING + "/item edit <champ> ...")
                .blank()
                .click(Tr.t("pour afficher la liste dans le chat"))
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
