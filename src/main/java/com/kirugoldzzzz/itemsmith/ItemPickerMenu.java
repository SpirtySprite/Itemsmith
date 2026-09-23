package com.kirugoldzzzz.itemsmith;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.gui.PaginatedGui;
import com.kirugoldzzzz.itemsmith.common.gui.DeferredPage;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class ItemPickerMenu {

    private static final int PAGE_SIZE = 45;

    private ItemPickerMenu() {
    }

    static <T> void open(Player player, String title, List<T> options, Function<T, ItemStack> icon,
                         BiConsumer<Player, T> pick, Runnable back) {
        PaginatedGui gui = PaginatedGui.builder()
                .rows(6)
                .title(ItemStyle.guiTitle(title))
                .create();
        DeferredPage<T> page = Guis.deferred(gui, options, PAGE_SIZE, option ->
                ItemBuilder.of(icon.apply(option)).asGuiItem(event -> {
                    Player viewer = (Player) event.getWhoClicked();
                    Guis.click(viewer);
                    pick.accept(viewer, option);
                }));
        Guis.paginationBar(gui, back);
        Guis.controls(gui, page);
        gui.open(player);
    }

    static ItemStack icon(Material material, String name, List<String> lore) {
        return decorate(new ItemStack(material), name, lore, meta -> {
        });
    }

    static ItemStack potion(Color color, String name, List<String> lore) {
        return decorate(new ItemStack(Material.POTION), name, lore, meta -> {
            if (meta instanceof PotionMeta potion && color != null) {
                potion.setColor(color);
            }
        });
    }

    private static ItemStack decorate(ItemStack stack, String name, List<String> lore, Consumer<ItemMeta> extra) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.displayName(Mini.label(name));
        meta.lore(Mini.labels(lore));
        meta.addItemFlags(ItemFlag.values());
        extra.accept(meta);
        stack.setItemMeta(meta);
        return stack;
    }
}
