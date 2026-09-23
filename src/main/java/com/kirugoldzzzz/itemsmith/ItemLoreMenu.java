package com.kirugoldzzzz.itemsmith;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.gui.PaginatedGui;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.DeferredPage;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

final class ItemLoreMenu {

    private static final int PAGE_SIZE = 45;
    private static final Component BAR = Component.text(Palette.PIPE + " ").color(TextColor.fromHexString(ItemStyle.HEX));

    private final ItemEditService service;
    private final Consumer<Player> back;

    ItemLoreMenu(ItemEditService service, Consumer<Player> back) {
        this.service = service;
        this.back = back;
    }

    void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        ItemStack item = service.held(player);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta != null && meta.hasLore() ? meta.lore() : List.of();
        PaginatedGui gui = PaginatedGui.builder()
                .rows(6)
                .title(ItemStyle.guiTitle("Description"))
                .create();
        List<Integer> lines = IntStream.rangeClosed(1, lore.size()).boxed().toList();
        DeferredPage<Integer> page = Guis.deferred(gui, lines, PAGE_SIZE,
                line -> lineIcon(line, lore.get(line - 1), lore.size()));
        Guis.paginationBar(gui, () -> back.accept(player));
        gui.setItem(6, 4, addButton(lore.size()));
        gui.setItem(6, 6, clearButton(lore.size()));
        Guis.controls(gui, page);
        gui.open(player);
    }

    private GuiItem lineIcon(int line, Component text, int total) {
        List<Component> lore = new ArrayList<>(Mini.labels(ItemStyle.card("Ligne " + line + " sur " + total)
                .section("Aperçu")
                .build()));
        lore.add(BAR.append(text));
        lore.addAll(Mini.labels(Card.of(ItemStyle.HEX)
                .blank()
                .click("Clic gauche", "pour modifier")
                .click("Clic droit", "pour supprimer")
                .click("Maj + clic gauche", "pour monter")
                .click("Maj + clic droit", "pour descendre")
                .click("Touche jeter", "pour insérer une ligne avant")
                .build()));
        return ItemBuilder.of(Material.PAPER)
                .name(Mini.label(ItemStyle.heading("Ligne " + line)))
                .loreComponents(lore)
                .asGuiItem(event -> {
                    Player player = (Player) event.getWhoClicked();
                    ClickType click = event.getClick();
                    switch (click) {
                        case SHIFT_LEFT -> apply(player, lore(lines -> LoreEdit.move(lines, line, Math.max(1, line - 1)),
                                "Ligne " + line + " montée"));
                        case SHIFT_RIGHT -> apply(player, lore(lines -> LoreEdit.move(lines, line, Math.min(total, line + 1)),
                                "Ligne " + line + " descendue"));
                        case RIGHT -> apply(player, lore(lines -> LoreEdit.remove(lines, line), "Ligne " + line + " supprimée"));
                        case DROP, CONTROL_DROP -> ItemPrompts.edit(service, player, "Nouvelle ligne", true,
                                typed -> lore(lines -> LoreEdit.insert(lines, line, ItemText.parse(typed)),
                                        "Ligne insérée en position " + line), () -> open(player));
                        default -> ItemPrompts.edit(service, player, "Ligne " + line, true,
                                typed -> lore(lines -> LoreEdit.set(lines, line, ItemText.parse(typed)),
                                        "Ligne " + line + " modifiée"), () -> open(player));
                    }
                });
    }

    private GuiItem addButton(int total) {
        return Guis.item(Material.LIME_DYE, ItemStyle.heading("Ajouter une ligne"), ItemStyle.card("Description")
                .blank()
                .count(Card.AMOUNT, "Lignes", total)
                .line("Couleurs MiniMessage ou codes &")
                .line(Palette.MUTED + "Texte long : /item edit lore add")
                .blank()
                .click("pour écrire une nouvelle ligne")
                .build(), false, event -> {
            Player player = (Player) event.getWhoClicked();
            ItemPrompts.edit(service, player, "Nouvelle ligne", true,
                    typed -> lore(lines -> LoreEdit.add(lines, ItemText.parse(typed)), "Ligne ajoutée"),
                    () -> open(player));
        });
    }

    private GuiItem clearButton(int total) {
        if (total == 0) {
            return ItemStyle.unavailable(Material.BARRIER, "Tout effacer", "La description est déjà vide");
        }
        return Guis.item(Material.BARRIER, Palette.ERROR + "<b>" + Card.small("Tout effacer") + "</b>",
                Card.of(Palette.ERROR_HEX)
                        .tag("Description")
                        .blank()
                        .line("Supprime les " + total + " lignes")
                        .line("Annulable avec /item undo")
                        .blank()
                        .click("Maj + clic", "pour confirmer")
                        .build(), false, event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!event.isShiftClick()) {
                        Guis.deny(player);
                        return;
                    }
                    apply(player, lore(lines -> List.of(), "Description effacée"));
                });
    }

    private static Function<ItemStack, Edit> lore(UnaryOperator<List<Component>> operation, String change) {
        return item -> ItemEditor.lore(item, operation, change);
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }
}
