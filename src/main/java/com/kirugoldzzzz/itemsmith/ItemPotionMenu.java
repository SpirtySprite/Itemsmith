package com.kirugoldzzzz.itemsmith;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.gui.PaginatedGui;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.DeferredPage;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Numbers;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

final class ItemPotionMenu {

    private static final int PAGE_SIZE = 45;
    private static final long MILLIS_PER_TICK = 50L;

    private final ItemEditService service;
    private final Consumer<Player> back;

    ItemPotionMenu(ItemEditService service, Consumer<Player> back) {
        this.service = service;
        this.back = back;
    }

    void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        if (!(service.held(player).getItemMeta() instanceof PotionMeta meta)) {
            service.fail(player, "Cet objet n'est pas une potion");
            back.accept(player);
            return;
        }
        List<PotionEffect> effects = List.copyOf(meta.getCustomEffects());
        PaginatedGui gui = PaginatedGui.builder()
                .rows(6)
                .title(ItemStyle.guiTitle("Effets de potion"))
                .create();
        DeferredPage<PotionEffect> page = Guis.deferred(gui, effects, PAGE_SIZE, this::effectIcon);
        Guis.paginationBar(gui, () -> back.accept(player));
        gui.setItem(6, 2, baseButton(meta));
        gui.setItem(6, 4, addButton());
        gui.setItem(6, 6, clearButton(effects.size()));
        Guis.controls(gui, page);
        gui.open(player);
    }

    private GuiItem effectIcon(PotionEffect effect) {
        PotionEffectType type = effect.getType();
        ItemStack icon = ItemPickerMenu.potion(type.getColor(),
                Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.effect(type)),
                ItemStyle.card("Effet personnalisé")
                        .section("Effet")
                        .stat(Card.STAR, "Niveau", effect.getAmplifier() + 1)
                        .stat(Card.TIME, "Durée", effect.isInfinite() ? "infinie"
                                : Numbers.duration(effect.getDuration() * MILLIS_PER_TICK))
                        .blank()
                        .click("Clic droit", "pour retirer")
                        .build());
        return ItemBuilder.of(icon).asGuiItem(event -> {
            Player player = (Player) event.getWhoClicked();
            if (!event.isRightClick()) {
                Guis.deny(player);
                return;
            }
            apply(player, item -> ItemEditor.removePotionEffect(item, type));
        });
    }

    private GuiItem baseButton(PotionMeta meta) {
        PotionType base = meta.getBasePotionType();
        return Guis.item(Material.GLASS_BOTTLE, ItemStyle.heading("Potion de base"), ItemStyle.card("Base")
                .blank()
                .stat(Card.FLAG, "Actuelle", base == null ? "aucune" : ItemLookup.shortKey(base))
                .blank()
                .click("Clic gauche", "pour choisir la base")
                .click("Clic droit", "pour la retirer")
                .build(), base != null, event -> {
            Player player = (Player) event.getWhoClicked();
            if (event.isRightClick()) {
                apply(player, item -> ItemEditor.potionType(item, null));
                return;
            }
            ItemPickerMenu.open(player, "Potion de base", ItemLookup.sorted(Registry.POTION),
                    type -> ItemPickerMenu.potion(type.getPotionEffects().isEmpty() ? null
                                    : type.getPotionEffects().getFirst().getType().getColor(),
                            Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.potionType(type)),
                            ItemStyle.card("Base")
                                    .blank()
                                    .stat(Card.FLAG, "Clé", ItemLookup.shortKey(type))
                                    .blank()
                                    .click("pour choisir cette base")
                                    .build()),
                    (viewer, type) -> apply(viewer, item -> ItemEditor.potionType(item, type)),
                    () -> open(player));
        });
    }

    private GuiItem addButton() {
        return Guis.item(Material.BREWING_STAND, ItemStyle.heading("Ajouter un effet"), ItemStyle.card("Nouvel effet")
                .section("Étapes")
                .line("1. Choisir l'effet")
                .line("2. Saisir la durée en secondes")
                .line("3. Saisir le niveau, jusqu'à " + ItemLookup.MAX_ENCHANT_LEVEL)
                .blank()
                .click("pour commencer")
                .build(), false, event -> pickEffect((Player) event.getWhoClicked()));
    }

    private GuiItem clearButton(int count) {
        if (count == 0) {
            return ItemStyle.unavailable(Material.MILK_BUCKET, "Tout retirer", "Aucun effet personnalisé");
        }
        return Guis.item(Material.MILK_BUCKET, Palette.ERROR + "<b>" + Card.small("Tout retirer") + "</b>",
                Card.of(Palette.ERROR_HEX)
                        .tag("Effets")
                        .blank()
                        .count(Card.STAR, "Effets personnalisés", count)
                        .blank()
                        .click("Maj + clic", "pour tout retirer")
                        .build(), false, event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!event.isShiftClick()) {
                        Guis.deny(player);
                        return;
                    }
                    apply(player, ItemEditor::clearPotion);
                });
    }

    private void pickEffect(Player player) {
        ItemPickerMenu.open(player, "Effet", ItemLookup.sorted(Registry.MOB_EFFECT),
                type -> ItemPickerMenu.potion(type.getColor(),
                        Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.effect(type)),
                        ItemStyle.card("Étape 1 sur 3")
                                .blank()
                                .stat(Card.FLAG, "Clé", ItemLookup.shortKey(type))
                                .blank()
                                .click("pour choisir cet effet")
                                .build()),
                (viewer, type) -> ItemPrompts.ask(viewer, "Durée (sec)", false, secondsText -> {
                    int seconds;
                    try {
                        seconds = ItemLookup.integer(secondsText, 1, Integer.MAX_VALUE / 20, "La durée");
                    } catch (EditException invalid) {
                        service.fail(viewer, invalid.getMessage());
                        open(viewer);
                        return;
                    }
                    ItemPrompts.edit(service, viewer, "Niveau 1 à 255", false, levelText -> {
                        int level = ItemLookup.integer(levelText, 1, ItemLookup.MAX_ENCHANT_LEVEL, "Le niveau");
                        return item -> ItemEditor.potionEffect(item, type, seconds, level);
                    }, () -> open(viewer));
                }, () -> open(viewer)),
                () -> open(player));
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }
}
