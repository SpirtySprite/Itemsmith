package com.kirugoldzzzz.itemsmith;

import com.kirugoldzzzz.itemsmith.common.text.Tr;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.Map;

final class ItemNaming {

    private static final Map<String, Material> TRIM_ICONS = Map.ofEntries(
            Map.entry("quartz", Material.QUARTZ), Map.entry("iron", Material.IRON_INGOT),
            Map.entry("netherite", Material.NETHERITE_INGOT), Map.entry("redstone", Material.REDSTONE),
            Map.entry("copper", Material.COPPER_INGOT), Map.entry("gold", Material.GOLD_INGOT),
            Map.entry("emerald", Material.EMERALD), Map.entry("diamond", Material.DIAMOND),
            Map.entry("lapis", Material.LAPIS_LAZULI), Map.entry("amethyst", Material.AMETHYST_SHARD),
            Map.entry("resin", Material.RESIN_BRICK));

    private ItemNaming() {
    }

    static String lang(String translationKey) {
        return "<lang:" + translationKey + ">";
    }

    static String enchantment(Enchantment enchantment) {
        NamespacedKey key = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getKeyOrThrow(enchantment);
        return lang("enchantment." + key.getNamespace() + "." + key.getKey());
    }

    static String attribute(Attribute attribute) {
        return lang(attribute.translationKey());
    }

    static String effect(PotionEffectType effect) {
        return lang(effect.translationKey());
    }

    static String potionType(PotionType type) {
        NamespacedKey key = type.getKey();
        return lang("item.minecraft.potion.effect." + key.getKey().replace("long_", "").replace("strong_", ""));
    }

    static String trimMaterial(TrimMaterial material) {
        NamespacedKey key = trimMaterialKey(material);
        return lang("trim_material." + key.getNamespace() + "." + key.getKey());
    }

    static String trimPattern(TrimPattern pattern) {
        NamespacedKey key = trimPatternKey(pattern);
        return lang("trim_pattern." + key.getNamespace() + "." + key.getKey());
    }

    private static NamespacedKey trimMaterialKey(TrimMaterial material) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).getKeyOrThrow(material);
    }

    private static NamespacedKey trimPatternKey(TrimPattern pattern) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).getKeyOrThrow(pattern);
    }

    static String flag(ItemFlag flag) {
        return switch (flag) {
            case HIDE_ENCHANTS -> Tr.t("Enchantements");
            case HIDE_ATTRIBUTES -> Tr.t("Attributs");
            case HIDE_UNBREAKABLE -> Tr.t("Incassable");
            case HIDE_DESTROYS -> Tr.t("Blocs cassables");
            case HIDE_PLACED_ON -> Tr.t("Blocs posables");
            case HIDE_ADDITIONAL_TOOLTIP -> Tr.t("Infos supplémentaires");
            case HIDE_DYE -> Tr.t("Teinture");
            case HIDE_ARMOR_TRIM -> Tr.t("Garniture");
            case HIDE_STORED_ENCHANTS -> Tr.t("Enchantements stockés");
        };
    }

    static Material flagIcon(ItemFlag flag) {
        return switch (flag) {
            case HIDE_ENCHANTS -> Material.ENCHANTED_BOOK;
            case HIDE_ATTRIBUTES -> Material.IRON_SWORD;
            case HIDE_UNBREAKABLE -> Material.BEDROCK;
            case HIDE_DESTROYS -> Material.DIAMOND_PICKAXE;
            case HIDE_PLACED_ON -> Material.GRASS_BLOCK;
            case HIDE_ADDITIONAL_TOOLTIP -> Material.BOOK;
            case HIDE_DYE -> Material.RED_DYE;
            case HIDE_ARMOR_TRIM -> Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
            case HIDE_STORED_ENCHANTS -> Material.KNOWLEDGE_BOOK;
        };
    }

    static String rarity(ItemRarity rarity) {
        if (rarity == null) {
            return Tr.t("Par défaut");
        }
        return switch (rarity) {
            case COMMON -> Tr.t("Commune");
            case UNCOMMON -> Tr.t("Peu commune");
            case RARE -> Tr.t("Rare");
            case EPIC -> Tr.t("Épique");
        };
    }

    static String operation(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_NUMBER -> Tr.t("Ajouter");
            case ADD_SCALAR -> Tr.t("Pourcentage de base");
            case MULTIPLY_SCALAR_1 -> Tr.t("Multiplier");
        };
    }

    static String slot(EquipmentSlotGroup group) {
        return switch (group.toString()) {
            case "any" -> Tr.t("Partout");
            case "mainhand" -> Tr.t("Main principale");
            case "offhand" -> Tr.t("Main secondaire");
            case "hand" -> Tr.t("Une main");
            case "head" -> Tr.t("Tête");
            case "chest" -> Tr.t("Torse");
            case "legs" -> Tr.t("Jambes");
            case "feet" -> Tr.t("Pieds");
            case "armor" -> Tr.t("Armure");
            case "body" -> Tr.t("Corps");
            case "saddle" -> Tr.t("Selle");
            default -> group.toString();
        };
    }

    static Material slotIcon(EquipmentSlotGroup group) {
        return switch (group.toString()) {
            case "mainhand" -> Material.IRON_SWORD;
            case "offhand" -> Material.SHIELD;
            case "hand" -> Material.STICK;
            case "head" -> Material.IRON_HELMET;
            case "chest" -> Material.IRON_CHESTPLATE;
            case "legs" -> Material.IRON_LEGGINGS;
            case "feet" -> Material.IRON_BOOTS;
            case "armor" -> Material.ARMOR_STAND;
            case "body" -> Material.IRON_HORSE_ARMOR;
            case "saddle" -> Material.SADDLE;
            default -> Material.NETHER_STAR;
        };
    }

    static Material attributeIcon(Attribute attribute) {
        String key = attribute.getKey().getKey();
        if (key.contains("damage")) {
            return Material.IRON_SWORD;
        }
        if (key.contains("armor")) {
            return Material.IRON_CHESTPLATE;
        }
        if (key.contains("health") || key.contains("absorption")) {
            return Material.GOLDEN_APPLE;
        }
        if (key.contains("speed")) {
            return Material.FEATHER;
        }
        if (key.contains("knockback")) {
            return Material.PISTON;
        }
        if (key.contains("luck")) {
            return Material.RABBIT_FOOT;
        }
        if (key.contains("scale")) {
            return Material.PUFFERFISH;
        }
        if (key.contains("jump") || key.contains("fall") || key.contains("gravity")) {
            return Material.SLIME_BALL;
        }
        if (key.contains("range") || key.contains("interaction")) {
            return Material.SPYGLASS;
        }
        if (key.contains("mining") || key.contains("break")) {
            return Material.DIAMOND_PICKAXE;
        }
        return Material.NETHER_STAR;
    }

    static Material trimMaterialIcon(TrimMaterial material) {
        return TRIM_ICONS.getOrDefault(trimMaterialKey(material).getKey(), Material.PAPER);
    }

    static Material trimPatternIcon(TrimPattern pattern) {
        Material template = Material.matchMaterial(trimPatternKey(pattern).getKey() + "_armor_trim_smithing_template");
        return template == null ? Material.PAPER : template;
    }
}
