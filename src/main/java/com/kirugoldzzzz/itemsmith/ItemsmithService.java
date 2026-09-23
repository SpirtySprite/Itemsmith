package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.api.ItemsmithApi;
import org.bukkit.entity.Player;

import java.util.UUID;

final class ItemsmithService implements ItemsmithApi {

    private final ItemEditService service;
    private final ItemEditMenu menu;

    ItemsmithService(ItemEditService service, ItemEditMenu menu) {
        this.service = service;
        this.menu = menu;
    }

    @Override
    public void openEditor(Player player) {
        menu.open(player);
    }

    @Override
    public boolean undo(Player player) {
        return service.undo(player);
    }

    @Override
    public int undoable(UUID player) {
        return service.undoable(player);
    }

    @Override
    public int historyDepth() {
        return ItemEditService.HISTORY_DEPTH;
    }
}
