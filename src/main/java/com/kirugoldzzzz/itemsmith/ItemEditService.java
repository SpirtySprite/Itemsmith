package com.kirugoldzzzz.itemsmith;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.scheduler.Scheduling;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.util.LruCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.function.Function;

public final class ItemEditService {

    static final int HISTORY_DEPTH = 20;
    private static final int TRACKED_PLAYERS = 256;

    private final LruCache<UUID, Deque<Change>> history = new LruCache<>(TRACKED_PLAYERS);

    public ItemStack held(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        return hand.getType().isAir() ? null : hand.clone();
    }

    public boolean holding(Player player) {
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            Guis.deny(player);
            Messages.send(player, "item-edit.no-item");
            return false;
        }
        return true;
    }

    public boolean apply(Player player, Function<ItemStack, Edit> change) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Guis.deny(player);
            Messages.send(player, "item-edit.no-item");
            return false;
        }
        ItemStack before = hand.clone();
        Edit edit;
        try {
            edit = change.apply(hand.clone());
        } catch (EditException invalid) {
            edit = Edit.failed(invalid.getMessage());
        } catch (RuntimeException refused) {
            edit = Edit.failed("Le jeu refuse cette modification : " + refused.getMessage());
        }
        if (!edit.success()) {
            fail(player, edit.message());
            return false;
        }
        player.getInventory().setItemInMainHand(edit.item());
        remember(player.getUniqueId(), new Change(before, player.getInventory().getItemInMainHand().clone()));
        Guis.success(player);
        Messages.send(player, "item-edit.applied", Mini.value("change", edit.message()));
        return true;
    }

    public void fail(Player player, String reason) {
        Guis.deny(player);
        Messages.send(player, "item-edit.error", Mini.value("reason", reason));
    }

    public boolean undo(Player player) {
        Deque<Change> stack = history.get(player.getUniqueId());
        Change last = stack == null ? null : peek(stack);
        if (last == null) {
            Guis.deny(player);
            Messages.send(player, "item-edit.nothing-to-undo");
            return false;
        }
        if (!last.after().equals(player.getInventory().getItemInMainHand()) || !remove(stack, last)) {
            fail(player, "Reprenez en main l'objet modifié pour annuler");
            return false;
        }
        player.getInventory().setItemInMainHand(last.before());
        Guis.success(player);
        Messages.send(player, "item-edit.undone", Mini.value("left", String.valueOf(undoable(player.getUniqueId()))));
        return true;
    }

    public int undoable(UUID player) {
        Deque<Change> stack = history.get(player);
        if (stack == null) {
            return 0;
        }
        synchronized (stack) {
            return stack.size();
        }
    }

    public void skullOf(Player player, String name, Runnable after) {
        Scheduling.async(() -> {
            PlayerProfile profile = Bukkit.createProfile(name);
            boolean found = profile.complete(true);
            Scheduling.entity(player, () -> {
                if (!found || !profile.hasTextures()) {
                    fail(player, "Aucun skin trouvé pour " + name);
                } else {
                    apply(player, item -> ItemEditor.skullProfile(item, profile));
                }
                if (after != null) {
                    after.run();
                }
            });
        });
    }

    private void remember(UUID player, Change change) {
        Deque<Change> stack = history.get(player);
        if (stack == null) {
            stack = new ArrayDeque<>();
            history.put(player, stack);
        }
        synchronized (stack) {
            stack.push(change);
            while (stack.size() > HISTORY_DEPTH) {
                stack.removeLast();
            }
        }
    }

    private static Change peek(Deque<Change> stack) {
        synchronized (stack) {
            return stack.peekFirst();
        }
    }

    private static boolean remove(Deque<Change> stack, Change change) {
        synchronized (stack) {
            return stack.peekFirst() == change && stack.pollFirst() == change;
        }
    }

    private record Change(ItemStack before, ItemStack after) {
    }
}
