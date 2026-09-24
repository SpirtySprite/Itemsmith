package com.kirugoldzzzz.itemsmith.common.log;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

public enum LogTopic {

    GENERAL("general", Tr.t("Général")),
    STORAGE("storage", Tr.t("Stockage")),
    CONFIG("config", Tr.t("Configuration")),
    MENUS("menus", Tr.t("Menus")),
    ITEMS("items", Tr.t("Objets"));

    private final String id;
    private final String label;

    LogTopic(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String permission() {
        return Alerts.ALL + "." + id;
    }
}
