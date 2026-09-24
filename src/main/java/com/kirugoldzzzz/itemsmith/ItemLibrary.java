package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.log.LogTopic;
import com.kirugoldzzzz.itemsmith.common.log.PluginLog;
import com.kirugoldzzzz.itemsmith.common.text.Tr;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class ItemLibrary {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final File file;
    private final Map<String, ItemStack> items = new TreeMap<>();

    public ItemLibrary(File file) {
        this.file = file;
    }

    public static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static String normalize(String id) {
        return id == null ? "" : id.strip().toLowerCase(Locale.ROOT);
    }

    public synchronized void load() {
        items.clear();
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ItemStack item = yaml.getItemStack(id);
            if (item != null && !item.getType().isAir()) {
                items.put(id, item);
            }
        }
    }

    public synchronized boolean save(String id, ItemStack item) {
        String key = normalize(id);
        if (!validId(key) || item == null || item.getType().isAir()) {
            return false;
        }
        ItemStack copy = item.clone();
        copy.setAmount(1);
        items.put(key, copy);
        persist();
        return true;
    }

    public synchronized Optional<ItemStack> get(String id) {
        ItemStack item = items.get(normalize(id));
        return item == null ? Optional.empty() : Optional.of(item.clone());
    }

    public synchronized boolean delete(String id) {
        boolean removed = items.remove(normalize(id)) != null;
        if (removed) {
            persist();
        }
        return removed;
    }

    public synchronized List<String> ids() {
        return new ArrayList<>(items.keySet());
    }

    public synchronized int size() {
        return items.size();
    }

    private void persist() {
        YamlConfiguration yaml = new YamlConfiguration();
        items.forEach((id, item) -> yaml.set(id, item.clone()));
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException failure) {
            PluginLog.warn(LogTopic.ITEMS, Tr.t("Bibliothèque d'objets non enregistrée : ") + failure.getMessage());
        }
    }
}
