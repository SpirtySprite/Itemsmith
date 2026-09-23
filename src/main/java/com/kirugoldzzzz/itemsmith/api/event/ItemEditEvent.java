package com.kirugoldzzzz.itemsmith.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemEditEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack before;
    private final ItemStack after;
    private final String change;
    private boolean cancelled;

    public ItemEditEvent(Player player, ItemStack before, ItemStack after, String change) {
        super(player, !Bukkit.isPrimaryThread());
        this.before = before.clone();
        this.after = after.clone();
        this.change = change;
    }

    public ItemStack before() {
        return before.clone();
    }

    public ItemStack after() {
        return after.clone();
    }

    public String change() {
        return change;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
