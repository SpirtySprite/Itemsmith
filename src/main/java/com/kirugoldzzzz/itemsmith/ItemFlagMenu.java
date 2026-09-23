package com.kirugoldzzzz.itemsmith;

import com.foliagui.gui.Gui;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;
import java.util.function.Function;

final class ItemFlagMenu {

    private final ItemEditService service;
    private final Consumer<Player> back;

    ItemFlagMenu(ItemEditService service, Consumer<Player> back) {
        this.service = service;
        this.back = back;
    }

    void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        ItemMeta meta = service.held(player).getItemMeta();
        Gui gui = Gui.builder()
                .rows(3)
                .title(ItemStyle.guiTitle("Masquages"))
                .create();
        Guis.fill(gui);
        ItemFlag[] flags = ItemFlag.values();
        for (int index = 0; index < flags.length && index < 9; index++) {
            ItemFlag flag = flags[index];
            boolean hidden = meta != null && meta.hasItemFlag(flag);
            gui.setItem(1, index + 1, Guis.item(ItemNaming.flagIcon(flag), ItemStyle.heading(ItemNaming.flag(flag)),
                    ItemStyle.card("Infobulle")
                            .blank()
                            .stat(Card.FLAG, "Affichage", hidden ? "masqué" : "visible")
                            .blank()
                            .click(hidden ? "pour afficher" : "pour masquer")
                            .build(), hidden, event -> apply((Player) event.getWhoClicked(),
                            item -> ItemEditor.flag(item, flag, null))));
        }
        gui.setItem(2, 4, Guis.item(Material.BLACK_DYE, ItemStyle.heading("Tout masquer"), ItemStyle.card("Infobulle")
                .blank()
                .click("pour masquer toutes les infos")
                .build(), false, event -> apply((Player) event.getWhoClicked(), item -> ItemEditor.allFlags(item, true))));
        gui.setItem(2, 6, Guis.item(Material.WHITE_DYE, ItemStyle.heading("Tout afficher"), ItemStyle.card("Infobulle")
                .blank()
                .click("pour afficher toutes les infos")
                .build(), false, event -> apply((Player) event.getWhoClicked(), item -> ItemEditor.allFlags(item, false))));
        gui.setItem(3, 1, Guis.backButton(() -> back.accept(player)));
        gui.setItem(3, 9, Guis.closeButton());
        gui.open(player);
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }
}
