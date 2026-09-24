package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import com.kirugoldzzzz.itemsmith.common.text.Tr;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

final class ItemLibraryMenu {

    private final ItemLibrary library;

    ItemLibraryMenu(ItemLibrary library) {
        this.library = library;
    }

    void open(Player player) {
        List<String> ids = library.ids();
        if (ids.isEmpty()) {
            Guis.deny(player);
            Messages.send(player, "item-edit.library-empty");
            return;
        }
        ItemPickerMenu.open(player, Tr.t("Bibliothèque"), ids, this::icon, (viewer, id) ->
                library.get(id).ifPresent(item -> {
                    give(viewer, item);
                    Guis.success(viewer);
                    Messages.send(viewer, "item-edit.library-loaded", Mini.value("id", id));
                }), null);
    }

    static void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    private ItemStack icon(String id) {
        ItemStack item = library.get(id).orElseGet(() -> new ItemStack(Material.BARRIER));
        return Guis.described(item, null, List.of(
                "",
                Palette.MUTED + Tr.t("Identifiant : ") + Palette.SECONDARY + id,
                Palette.MUTED + Tr.t("Clic pour en recevoir une copie")));
    }
}
