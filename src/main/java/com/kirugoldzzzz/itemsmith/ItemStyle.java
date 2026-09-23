package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import com.foliagui.item.GuiAction;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Mini;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

final class ItemStyle {

    static final String HEX = "#22D3EE";
    static final String ACCENT = "#A78BFA";

    private ItemStyle() {
    }

    static Card card(String tag) {
        return Card.of(HEX).tag(tag);
    }

    static String heading(String text) {
        return Card.title(HEX, ACCENT, Card.small(text));
    }

    static String title(String text) {
        return Card.title(HEX, ACCENT, Card.small(Tr.t("Éditeur"))) + Palette.MUTED + Tr.t(" » ") + Palette.TEXT + text;
    }

    static String state(boolean active) {
        return active ? Palette.SUCCESS + Tr.t("Activé") : Palette.MUTED + Tr.t("Désactivé");
    }

    static GuiItem button(Material material, String name, Card card, boolean glow,
                          GuiAction<InventoryClickEvent> action) {
        return Guis.item(material, heading(name), card.build(), glow, action);
    }

    static GuiItem unavailable(Material material, String name, String reason) {
        return Guis.display(material, Palette.MUTED + "<b>" + Card.small(name) + "</b>",
                Card.of(Palette.MUTED_HEX).tag(Tr.t("Indisponible")).blank().deny(reason).build());
    }

    static Component guiTitle(String text) {
        return Mini.parse(title(text));
    }
}
