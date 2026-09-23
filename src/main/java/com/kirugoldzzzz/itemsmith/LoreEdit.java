package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

final class LoreEdit {

    static final int MAX_LINES = 256;

    private LoreEdit() {
    }

    static List<Component> add(List<Component> lore, Component line) {
        List<Component> copy = copy(lore);
        ensureRoom(copy);
        copy.add(line);
        return copy;
    }

    static List<Component> set(List<Component> lore, int line, Component value) {
        List<Component> copy = copy(lore);
        copy.set(index(copy, line), value);
        return copy;
    }

    static List<Component> insert(List<Component> lore, int line, Component value) {
        List<Component> copy = copy(lore);
        ensureRoom(copy);
        if (line < 1 || line > copy.size() + 1) {
            throw new EditException("Position " + line + Tr.t(" invalide, choisissez entre 1 et ") + (copy.size() + 1));
        }
        copy.add(line - 1, value);
        return copy;
    }

    static List<Component> remove(List<Component> lore, int line) {
        List<Component> copy = copy(lore);
        copy.remove(index(copy, line));
        return copy;
    }

    static List<Component> move(List<Component> lore, int from, int to) {
        List<Component> copy = copy(lore);
        Component moved = copy.remove(index(copy, from));
        if (to < 1 || to > copy.size() + 1) {
            throw new EditException("Position " + to + Tr.t(" invalide, choisissez entre 1 et ") + (copy.size() + 1));
        }
        copy.add(to - 1, moved);
        return copy;
    }

    private static int index(List<Component> lore, int line) {
        if (lore.isEmpty()) {
            throw new EditException(Tr.t("La description est vide"));
        }
        if (line < 1 || line > lore.size()) {
            throw new EditException(Tr.t("La ligne ") + line + Tr.t(" n'existe pas, choisissez entre 1 et ") + lore.size());
        }
        return line - 1;
    }

    private static void ensureRoom(List<Component> lore) {
        if (lore.size() >= MAX_LINES) {
            throw new EditException(Tr.t("La description ne peut pas dépasser ") + MAX_LINES + " lignes");
        }
    }

    private static List<Component> copy(List<Component> lore) {
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }
}
