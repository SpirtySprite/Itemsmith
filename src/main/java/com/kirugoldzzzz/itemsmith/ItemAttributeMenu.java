package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import com.foliagui.gui.PaginatedGui;
import com.foliagui.item.GuiItem;
import com.kirugoldzzzz.itemsmith.common.gui.DeferredPage;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.text.Card;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

final class ItemAttributeMenu {

    private static final int PAGE_SIZE = 45;
    private static final List<AttributeModifier.Operation> OPERATIONS = List.of(AttributeModifier.Operation.values());

    private final ItemEditService service;
    private final Consumer<Player> back;

    ItemAttributeMenu(ItemEditService service, Consumer<Player> back) {
        this.service = service;
        this.back = back;
    }

    void open(Player player) {
        if (!service.holding(player)) {
            return;
        }
        ItemMeta meta = service.held(player).getItemMeta();
        List<Map.Entry<Attribute, AttributeModifier>> modifiers = new ArrayList<>();
        if (meta != null && meta.hasAttributeModifiers()) {
            modifiers.addAll(meta.getAttributeModifiers().entries());
        }
        PaginatedGui gui = PaginatedGui.builder()
                .rows(6)
                .title(ItemStyle.guiTitle(Tr.t("Attributs")))
                .create();
        DeferredPage<Map.Entry<Attribute, AttributeModifier>> page = Guis.deferred(gui, modifiers, PAGE_SIZE,
                entry -> modifierIcon(entry.getKey(), entry.getValue()));
        Guis.paginationBar(gui, () -> back.accept(player));
        gui.setItem(6, 4, addButton());
        gui.setItem(6, 6, resetButton(!modifiers.isEmpty()));
        if (modifiers.isEmpty()) {
            gui.setItem(3, 5, Guis.display(Material.STRUCTURE_VOID, ItemStyle.heading(Tr.t("Attributs par défaut")),
                    ItemStyle.card(Tr.t("Attributs"))
                            .section(Tr.t("Description"))
                            .line(Tr.t("L'objet garde ses attributs d'origine."))
                            .line(Tr.t("Ajouter un modificateur les remplace tous,"))
                            .line(Tr.t("comme en vanilla."))
                            .build()));
        }
        Guis.controls(gui, page);
        gui.open(player);
    }

    private GuiItem modifierIcon(Attribute attribute, AttributeModifier modifier) {
        return Guis.item(ItemNaming.attributeIcon(attribute),
                Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.attribute(attribute)),
                ItemStyle.card(Tr.t("Modificateur"))
                        .section(Tr.t("Valeur"))
                        .stat(modifier.getAmount() >= 0 ? Palette.SUCCESS : Palette.ERROR, Card.STAR, Tr.t("Montant"),
                                format(modifier.getAmount()))
                        .stat(Card.FLAG, Tr.t("Opération"), ItemNaming.operation(modifier.getOperation()))
                        .stat(Card.CATEGORY, Tr.t("Emplacement"), ItemNaming.slot(modifier.getSlotGroup()))
                        .blank()
                        .click(Tr.t("Clic droit"), Tr.t("pour retirer"))
                        .build(), false, event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!event.isRightClick()) {
                        Guis.deny(player);
                        return;
                    }
                    apply(player, item -> ItemEditor.removeModifier(item, attribute, modifier));
                });
    }

    private GuiItem addButton() {
        return Guis.item(Material.LIME_DYE, ItemStyle.heading(Tr.t("Ajouter")), ItemStyle.card(Tr.t("Nouveau modificateur"))
                .section(Tr.t("Étapes"))
                .line("1. Choisir l'attribut")
                .line(Tr.t("2. Saisir la valeur"))
                .line(Tr.t("3. Choisir l'opération"))
                .line("4. Choisir l'emplacement")
                .blank()
                .click(Tr.t("pour commencer"))
                .build(), false, event -> pickAttribute((Player) event.getWhoClicked()));
    }

    private GuiItem resetButton(boolean customised) {
        if (!customised) {
            return ItemStyle.unavailable(Material.BARRIER, Tr.t("Réinitialiser"), Tr.t("Les attributs sont déjà par défaut"));
        }
        return Guis.item(Material.BARRIER, Palette.ERROR + "<b>" + Card.small(Tr.t("Réinitialiser")) + "</b>",
                Card.of(Palette.ERROR_HEX)
                        .tag(Tr.t("Attributs"))
                        .blank()
                        .line(Tr.t("Retire tous les modificateurs et rend"))
                        .line(Tr.t("à l'objet ses attributs d'origine."))
                        .blank()
                        .click(Tr.t("Maj + clic"), Tr.t("pour confirmer"))
                        .build(), false, event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (!event.isShiftClick()) {
                        Guis.deny(player);
                        return;
                    }
                    apply(player, ItemEditor::resetAttributes);
                });
    }

    private void pickAttribute(Player player) {
        ItemPickerMenu.open(player, Tr.t("Attribut"), ItemLookup.sorted(Registry.ATTRIBUTE),
                attribute -> ItemPickerMenu.icon(ItemNaming.attributeIcon(attribute),
                        Card.title(ItemStyle.HEX, ItemStyle.ACCENT, ItemNaming.attribute(attribute)),
                        ItemStyle.card(Tr.t("Étape 1 sur 4"))
                                .blank()
                                .stat(Card.FLAG, Tr.t("Clé"), ItemLookup.shortKey(attribute))
                                .blank()
                                .click(Tr.t("pour choisir cet attribut"))
                                .build()),
                (viewer, attribute) -> ItemPrompts.ask(viewer, Tr.t("Valeur"), false, typed -> {
                    double amount;
                    try {
                        amount = ItemLookup.decimal(typed, Tr.t("La valeur"));
                    } catch (EditException invalid) {
                        service.fail(viewer, invalid.getMessage());
                        open(viewer);
                        return;
                    }
                    pickOperation(viewer, attribute, amount);
                }, () -> open(viewer)),
                () -> open(player));
    }

    private void pickOperation(Player player, Attribute attribute, double amount) {
        ItemPickerMenu.open(player, Tr.t("Opération"), OPERATIONS,
                operation -> ItemPickerMenu.icon(operationIcon(operation),
                        ItemStyle.heading(ItemNaming.operation(operation)),
                        ItemStyle.card(Tr.t("Étape 3 sur 4"))
                                .section(Tr.t("Description"))
                                .line(operationHelp(operation))
                                .blank()
                                .click(Tr.t("pour choisir cette opération"))
                                .build()),
                (viewer, operation) -> pickSlot(viewer, attribute, amount, operation),
                () -> open(player));
    }

    private void pickSlot(Player player, Attribute attribute, double amount, AttributeModifier.Operation operation) {
        ItemPickerMenu.open(player, Tr.t("Emplacement"), ItemLookup.SLOT_GROUPS,
                slot -> ItemPickerMenu.icon(ItemNaming.slotIcon(slot), ItemStyle.heading(ItemNaming.slot(slot)),
                        ItemStyle.card(Tr.t("Étape 4 sur 4"))
                                .section(Tr.t("Récapitulatif"))
                                .stat(Card.STAR, Tr.t("Montant"), format(amount))
                                .stat(Card.FLAG, Tr.t("Opération"), ItemNaming.operation(operation))
                                .blank()
                                .click(Tr.t("pour appliquer sur cet emplacement"))
                                .build()),
                (viewer, slot) -> {
                    service.apply(viewer, item -> ItemEditor.addAttribute(item, attribute, amount, operation, slot));
                    open(viewer);
                },
                () -> open(player));
    }

    private static Material operationIcon(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_NUMBER -> Material.LIME_DYE;
            case ADD_SCALAR -> Material.YELLOW_DYE;
            case MULTIPLY_SCALAR_1 -> Material.ORANGE_DYE;
        };
    }

    private static String operationHelp(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_NUMBER -> Tr.t("Ajoute la valeur telle quelle");
            case ADD_SCALAR -> Tr.t("Ajoute un pourcentage de la valeur de base");
            case MULTIPLY_SCALAR_1 -> Tr.t("Multiplie le total par 1 + la valeur");
        };
    }

    static String format(double amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.valueOf(amount);
    }

    private void apply(Player player, Function<ItemStack, Edit> change) {
        service.apply(player, change);
        open(player);
    }
}
