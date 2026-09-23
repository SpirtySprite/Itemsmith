package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Mini;
import net.foliaboard.api.text.Legacy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

final class ItemText {

    private ItemText() {
    }

    static Component parse(String raw) {
        return Mini.label(Legacy.toMini(raw == null ? "" : raw));
    }

    static String plain(Component component) {
        return component == null ? "" : Mini.plain(component);
    }

    static String mini(Component component) {
        return component == null ? "" : MiniMessage.miniMessage().serialize(component);
    }

    static String join(String[] args, int from) {
        StringBuilder text = new StringBuilder();
        for (int index = from; index < args.length; index++) {
            if (index > from) {
                text.append(' ');
            }
            text.append(args[index]);
        }
        return text.toString();
    }
}
