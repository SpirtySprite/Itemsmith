package com.kirugoldzzzz.itemsmith.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public interface ItemsmithApi {

    static Optional<ItemsmithApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(ItemsmithApi.class));
    }

    void openEditor(Player player);

    boolean undo(Player player);

    int undoable(UUID player);

    int historyDepth();
}
