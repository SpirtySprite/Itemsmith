package com.kirugoldzzzz.itemsmith;

import org.bukkit.inventory.ItemStack;

public record Edit(boolean success, ItemStack item, String message) {

    static Edit done(ItemStack item, String change) {
        return new Edit(true, item, change);
    }

    static Edit failed(String reason) {
        return new Edit(false, null, reason);
    }
}
