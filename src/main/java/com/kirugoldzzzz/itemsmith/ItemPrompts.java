package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;

final class ItemPrompts {

    private ItemPrompts() {
    }

    static void edit(ItemEditService service, Player player, String label, boolean longText,
                     Function<String, Function<ItemStack, Edit>> change, Runnable reopen) {
        ask(player, label, longText, input -> {
            try {
                service.apply(player, change.apply(input));
            } catch (EditException invalid) {
                service.fail(player, invalid.getMessage());
            }
            reopen.run();
        }, reopen);
    }

    static void ask(Player player, String label, boolean longText, Consumer<String> handler, Runnable cancel) {
        player.closeInventory();
        Consumer<String> wrapped = input -> {
            if (input.isBlank()) {
                if (cancel != null) {
                    cancel.run();
                }
                return;
            }
            handler.accept(input);
        };
        if (longText) {
            Guis.promptLongSign(player, label, wrapped);
        } else {
            Guis.promptSign(player, label, wrapped);
        }
    }
}
