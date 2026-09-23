package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import com.foliagui.gui.PaginatedGui;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.DeferredPage;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

final class ItemEnchantMenu {

    private static final int PAGE_SIZE = 45;

    private final ItemEditService service;
    private final Consumer<Player> back;

    ItemEnchantMenu(ItemEditService service, Consumer<Player> back) {
        this.service = service;
        this.back = back;
    }

    void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        ItemStack item = service.held(player);
        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> current = meta == null ? Map.of() : meta.getEnchants();
        PaginatedGui gui = PaginatedGui.builder()
                .rows(6)
                .title(ItemStyle.guiTitle(Tr.t("Enchantements")))
                .create();
        List<Enchantment> enchantments = ItemLookup.sorted(Registry.ENCHANTMENT);
        DeferredPage<Enchantment> page = Guis.deferred(gui, enchantments, PAGE_SIZE,
                enchantment -> icon(item, enchantment, current.getOrDefault(enchantment, 0)));
        Guis.paginationBar(gui, () -> back.accept(player));
        gui.setItem(6, 4, clearButton(current.size()));
        gui.setItem(6, 6, Guis.display(Material.KNOWLEDGE_BOOK, ItemStyle.heading(Tr.t("Sans limite")), ItemStyle.card(Tr.t("Info"))
                .section(Tr.t("Description"))
                .line(Tr.t("Tous les enchantements s'appliquent"))
                .line(Tr.t("sur n'importe quel objet, sans conflit"))
                .line(Tr.t("et jusqu'au niveau ") + ItemLookup.MAX_ENCHANT_LEVEL + Tr.t(", le maximum du jeu."))
                .build()));
        Guis.controls(gui, page);
        gui.open(player);
    }

    private GuiItem icon(ItemStack item, Enchantment enchantment, int level) {
        boolean applied = level > 0;
        Card card = ItemStyle.card(applied ? Tr.t("Appliqué") : Tr.t("Disponible"))
                .blank()
                .stat(applied ? Palette.SUCCESS : Palette.MUTED, Card.STAR, Tr.t("Niveau actuel"), applied ? level : Tr.t("aucun"))
                .stat(Card.FLAG, Tr.t("Maximum normal"), enchantment.getMaxLevel())
                .stat(Card.CATEGORY, Tr.t("Adapté à cet objet"), enchantment.canEnchantItem(item) ? Tr.t("oui") : Tr.t("non, forcé quand même"))
                .blank()
                .click(Tr.t("Clic gauche"), Tr.t("pour ajouter un niveau"))
                .click(Tr.t("Clic droit"), Tr.t("pour retirer un niveau"))
                .click(Tr.t("Maj + clic gauche"), Tr.t("pour choisir le niveau"))
                .click(Tr.t("Maj + clic droit"), Tr.t("pour le retirer"));
        return Guis.item(applied ? Material.ENCHANTED_BOOK : Material.BOOK,
                Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.enchantment(enchantment)
                        + (applied ? " " + level : "")),
                card.build(), applied, event -> {
                    Player player = (Player) event.getWhoClicked();
                    switch (event.getClick()) {
                        case SHIFT_LEFT -> ItemPrompts.edit(service, player, Tr.t("Niveau 1 à 255"), false, typed -> {
                            int chosen = ItemLookup.integer(typed, 1, ItemLookup.MAX_ENCHANT_LEVEL, Tr.t("Le niveau"));
                            return held -> ItemEditor.enchant(held, enchantment, chosen);
                        }, () -> open(player));
                        case SHIFT_RIGHT -> apply(player, held -> ItemEditor.unenchant(held, enchantment));
                        case RIGHT -> apply(player, held -> level <= 1
                                ? ItemEditor.unenchant(held, enchantment)
                                : ItemEditor.enchant(held, enchantment, level - 1));
                        default -> apply(player, held -> ItemEditor.enchant(held, enchantment,
                                Math.min(ItemLookup.MAX_ENCHANT_LEVEL, level + 1)));
                    }
                });
    }

    private GuiItem clearButton(int count) {
        if (count == 0) {
            return ItemStyle.unavailable(Material.GRINDSTONE, Tr.t("Tout retirer"), Tr.t("Aucun enchantement appliqué"));
        }
        return Guis.item(Material.GRINDSTONE, Palette.ERROR + "<b>" + Card.small(Tr.t("Tout retirer")) + "</b>",
                Card.of(Palette.ERROR_HEX)
                        .tag(Tr.t("Enchantements"))
                        .blank()
                        .count(Card.STAR, Tr.t("Appliqués"), count)
                        .blank()
                        .click(Tr.t("Maj + clic"), Tr.t("pour tout retirer"))
                        .build(), false, event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!event.isShiftClick()) {
                        Guis.deny(player);
                        return;
                    }
                    apply(player, ItemEditor::clearEnchants);
                });
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }
}
