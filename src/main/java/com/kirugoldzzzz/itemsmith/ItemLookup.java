package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ItemLookup {

    static final int MAX_ENCHANT_LEVEL = 255;
    static final int MAX_STACK = 99;

    static final List<EquipmentSlotGroup> SLOT_GROUPS = List.of(EquipmentSlotGroup.ANY, EquipmentSlotGroup.MAINHAND,
            EquipmentSlotGroup.OFFHAND, EquipmentSlotGroup.HAND, EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST,
            EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET, EquipmentSlotGroup.ARMOR, EquipmentSlotGroup.BODY,
            EquipmentSlotGroup.SADDLE);

    private static final Map<String, AttributeModifier.Operation> OPERATIONS = Map.ofEntries(
            Map.entry("add", AttributeModifier.Operation.ADD_NUMBER),
            Map.entry("ajouter", AttributeModifier.Operation.ADD_NUMBER),
            Map.entry("add_number", AttributeModifier.Operation.ADD_NUMBER),
            Map.entry("+", AttributeModifier.Operation.ADD_NUMBER),
            Map.entry("percent", AttributeModifier.Operation.ADD_SCALAR),
            Map.entry("pourcentage", AttributeModifier.Operation.ADD_SCALAR),
            Map.entry("add_scalar", AttributeModifier.Operation.ADD_SCALAR),
            Map.entry("%", AttributeModifier.Operation.ADD_SCALAR),
            Map.entry("multiply", AttributeModifier.Operation.MULTIPLY_SCALAR_1),
            Map.entry("multiplier", AttributeModifier.Operation.MULTIPLY_SCALAR_1),
            Map.entry("multiply_scalar_1", AttributeModifier.Operation.MULTIPLY_SCALAR_1),
            Map.entry("x", AttributeModifier.Operation.MULTIPLY_SCALAR_1));

    private ItemLookup() {
    }

    static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    static NamespacedKey key(String raw) {
        NamespacedKey key = NamespacedKey.fromString(normalize(raw));
        if (key == null) {
            throw new EditException(Tr.t("Clé invalide \"") + raw + Tr.t("\", attendu espace:nom"));
        }
        return key;
    }

    static Enchantment enchantment(String raw) {
        return registered(Registry.ENCHANTMENT, raw, Tr.t("Enchantement inconnu"));
    }

    static Attribute attribute(String raw) {
        String value = normalize(raw);
        for (String legacy : List.of("generic.", "player.", "zombie.", "horse.")) {
            if (value.startsWith(legacy)) {
                value = value.substring(legacy.length());
            }
        }
        return registered(Registry.ATTRIBUTE, value, Tr.t("Attribut inconnu"));
    }

    static PotionEffectType effect(String raw) {
        return registered(Registry.MOB_EFFECT, raw, Tr.t("Effet inconnu"));
    }

    static PotionType potionType(String raw) {
        return registered(Registry.POTION, raw, Tr.t("Type de potion inconnu"));
    }

    static TrimMaterial trimMaterial(String raw) {
        return registered(Registry.TRIM_MATERIAL, raw, Tr.t("Matériau de garniture inconnu"));
    }

    static TrimPattern trimPattern(String raw) {
        return registered(Registry.TRIM_PATTERN, raw, Tr.t("Motif de garniture inconnu"));
    }

    static AttributeModifier.Operation operation(String raw) {
        if (raw == null || raw.isBlank()) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
        AttributeModifier.Operation operation = OPERATIONS.get(normalize(raw));
        if (operation == null) {
            throw new EditException(Tr.t("Opération inconnue \"") + raw + Tr.t("\", attendu add, percent ou multiply"));
        }
        return operation;
    }

    static EquipmentSlotGroup slot(String raw) {
        if (raw == null || raw.isBlank()) {
            return EquipmentSlotGroup.ANY;
        }
        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(normalize(raw));
        if (group == null) {
            throw new EditException(Tr.t("Emplacement inconnu \"") + raw + "\"");
        }
        return group;
    }

    static Material material(String raw) {
        Material material = Material.matchMaterial(normalize(raw));
        if (material == null || !material.isItem() || material.isAir()) {
            throw new EditException(Tr.t("Matériau inconnu \"") + raw + "\"");
        }
        return material;
    }

    static ItemFlag flag(String raw) {
        String value = normalize(raw).toUpperCase(Locale.ROOT).replace('-', '_');
        String prefixed = value.startsWith("HIDE_") ? value : "HIDE_" + value;
        try {
            return ItemFlag.valueOf(prefixed);
        } catch (IllegalArgumentException unknown) {
            throw new EditException(Tr.t("Masquage inconnu \"") + raw + "\"");
        }
    }

    static ItemRarity rarity(String raw) {
        return switch (normalize(raw).replace('-', '_')) {
            case "common", "commun", "commune" -> ItemRarity.COMMON;
            case "uncommon", "peu_commun", "peu_commune" -> ItemRarity.UNCOMMON;
            case "rare" -> ItemRarity.RARE;
            case "epic", "epique", "épique" -> ItemRarity.EPIC;
            default -> throw new EditException(Tr.t("Rareté inconnue \"") + raw + Tr.t("\", attendu common, uncommon, rare ou epic"));
        };
    }

    static boolean state(String raw, boolean current) {
        if (raw == null || raw.isBlank()) {
            return !current;
        }
        return switch (normalize(raw)) {
            case "on", "true", "oui", "yes", "1", "vrai" -> true;
            case "off", "false", "non", "no", "0", "faux" -> false;
            default -> throw new EditException("Valeur \"" + raw + Tr.t("\" invalide, attendu on ou off"));
        };
    }

    static boolean reset(String raw) {
        return switch (normalize(raw)) {
            case "reset", "default", "defaut", "défaut", "none", "aucun" -> true;
            default -> false;
        };
    }

    static int integer(String raw, int min, int max, String label) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.trim());
            if (value < min || value > max) {
                throw new EditException(label + Tr.t(" doit être entre ") + min + Tr.t(" et ") + max);
            }
            return value;
        } catch (NumberFormatException invalid) {
            throw new EditException(label + Tr.t(" doit être un nombre entier entre ") + min + Tr.t(" et ") + max);
        }
    }

    static double decimal(String raw, String label) {
        try {
            double value = Double.parseDouble(raw == null ? "" : raw.trim().replace(',', '.'));
            if (!Double.isFinite(value)) {
                throw new EditException(label + Tr.t(" doit être un nombre"));
            }
            return value;
        } catch (NumberFormatException invalid) {
            throw new EditException(label + Tr.t(" doit être un nombre"));
        }
    }

    static Color color(String raw) {
        String value = normalize(raw);
        NamedTextColor named = NamedTextColor.NAMES.value(value);
        if (named != null) {
            return Color.fromRGB(named.value());
        }
        String hex = value.startsWith("#") ? value : "#" + value;
        TextColor parsed = hex.length() == 7 ? TextColor.fromHexString(hex) : null;
        if (parsed == null) {
            throw new EditException(Tr.t("Couleur invalide \"") + raw + Tr.t("\", attendu #RRGGBB ou un nom comme red"));
        }
        return Color.fromRGB(parsed.value());
    }

    static <T extends Keyed> List<T> sorted(Registry<T> registry) {
        List<T> values = new ArrayList<>();
        registry.forEach(values::add);
        values.sort(Comparator.comparing(value -> value.getKey().toString()));
        return values;
    }

    static String shortKey(Keyed keyed) {
        NamespacedKey key = keyed.getKey();
        return NamespacedKey.MINECRAFT.equals(key.getNamespace()) ? key.getKey() : key.toString();
    }

    static <T extends Keyed> List<String> keys(Registry<T> registry) {
        List<String> keys = new ArrayList<>();
        for (T value : sorted(registry)) {
            keys.add(shortKey(value));
        }
        return keys;
    }

    private static <T extends Keyed> T registered(Registry<T> registry, String raw, String unknown) {
        NamespacedKey key = NamespacedKey.fromString(normalize(raw));
        T value = key == null ? null : registry.get(key);
        if (value == null) {
            throw new EditException(unknown + " \"" + raw + "\"");
        }
        return value;
    }
}
