package com.kirugoldzzzz.itemsmith;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.foliagui.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

final class ItemEditor {

    private static final int TICKS_PER_SECOND = 20;

    private ItemEditor() {
    }

    static Edit meta(ItemStack item, Function<ItemMeta, String> change) {
        return typed(item, ItemMeta.class, "Cet objet ne peut pas être modifié", change);
    }

    static <T> Edit typed(ItemStack item, Class<T> type, String unsupported, Function<T, String> change) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (!type.isInstance(meta)) {
                return Edit.failed(unsupported);
            }
            String description = change.apply(type.cast(meta));
            item.setItemMeta(meta);
            return Edit.done(item, description);
        } catch (EditException invalid) {
            return Edit.failed(invalid.getMessage());
        }
    }

    static Edit name(ItemStack item, String raw) {
        return meta(item, meta -> {
            meta.displayName(ItemText.parse(raw));
            return "Nom modifié";
        });
    }

    static Edit resetName(ItemStack item) {
        return meta(item, meta -> {
            meta.displayName(null);
            return "Nom réinitialisé";
        });
    }

    static Edit lore(ItemStack item, UnaryOperator<List<Component>> operation, String change) {
        return meta(item, meta -> {
            List<Component> updated = operation.apply(meta.lore());
            meta.lore(updated.isEmpty() ? null : updated);
            return change;
        });
    }

    static Edit enchant(ItemStack item, Enchantment enchantment, int level) {
        return meta(item, meta -> {
            if (level < 1 || level > ItemLookup.MAX_ENCHANT_LEVEL) {
                throw new EditException("Le niveau doit être entre 1 et " + ItemLookup.MAX_ENCHANT_LEVEL);
            }
            meta.removeEnchant(enchantment);
            meta.addEnchant(enchantment, level, true);
            return "Enchantement " + ItemLookup.shortKey(enchantment) + " niveau " + level;
        });
    }

    static Edit unenchant(ItemStack item, Enchantment enchantment) {
        return meta(item, meta -> {
            if (!meta.removeEnchant(enchantment)) {
                throw new EditException("Cet objet n'a pas l'enchantement " + ItemLookup.shortKey(enchantment));
            }
            return "Enchantement " + ItemLookup.shortKey(enchantment) + " retiré";
        });
    }

    static Edit clearEnchants(ItemStack item) {
        return meta(item, meta -> {
            meta.removeEnchantments();
            return "Tous les enchantements retirés";
        });
    }

    static Edit flag(ItemStack item, ItemFlag flag, String state) {
        return meta(item, meta -> {
            boolean hidden = ItemLookup.state(state, meta.hasItemFlag(flag));
            if (hidden) {
                meta.addItemFlags(flag);
            } else {
                meta.removeItemFlags(flag);
            }
            return (hidden ? "Masqué : " : "Affiché : ") + ItemNaming.flag(flag);
        });
    }

    static Edit allFlags(ItemStack item, boolean hidden) {
        return meta(item, meta -> {
            if (hidden) {
                meta.addItemFlags(ItemFlag.values());
            } else {
                meta.removeItemFlags(ItemFlag.values());
            }
            return hidden ? "Toutes les infos masquées" : "Toutes les infos affichées";
        });
    }

    static Edit addAttribute(ItemStack item, Attribute attribute, double amount,
                             AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        return meta(item, meta -> {
            NamespacedKey key = new NamespacedKey("itemsmith", "edit_" + UUID.randomUUID().toString().substring(0, 8));
            meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, operation, slot));
            return "Attribut " + ItemLookup.shortKey(attribute) + " " + amount + " ajouté";
        });
    }

    static Edit removeAttribute(ItemStack item, Attribute attribute) {
        return meta(item, meta -> {
            if (!meta.removeAttributeModifier(attribute)) {
                throw new EditException("Cet objet n'a pas d'attribut " + ItemLookup.shortKey(attribute));
            }
            return "Attribut " + ItemLookup.shortKey(attribute) + " retiré";
        });
    }

    static Edit removeModifier(ItemStack item, Attribute attribute, AttributeModifier modifier) {
        return meta(item, meta -> {
            if (!meta.removeAttributeModifier(attribute, modifier)) {
                throw new EditException("Ce modificateur n'existe plus");
            }
            return "Modificateur " + ItemLookup.shortKey(attribute) + " retiré";
        });
    }

    static Edit resetAttributes(ItemStack item) {
        return meta(item, meta -> {
            meta.setAttributeModifiers(null);
            return "Attributs remis par défaut";
        });
    }

    static Edit amount(ItemStack item, int amount) {
        if (amount < 1 || amount > ItemLookup.MAX_STACK) {
            return Edit.failed("La quantité doit être entre 1 et " + ItemLookup.MAX_STACK);
        }
        if (amount > item.getMaxStackSize()) {
            Edit widened = maxStack(item, amount);
            if (!widened.success()) {
                return widened;
            }
        }
        item.setAmount(amount);
        return Edit.done(item, "Quantité réglée sur " + amount);
    }

    static Edit maxStack(ItemStack item, Integer size) {
        return meta(item, meta -> {
            if (size != null && (size < 1 || size > ItemLookup.MAX_STACK)) {
                throw new EditException("La taille de pile doit être entre 1 et " + ItemLookup.MAX_STACK);
            }
            meta.setMaxStackSize(size);
            return size == null ? "Taille de pile par défaut" : "Taille de pile réglée sur " + size;
        });
    }

    static Edit damage(ItemStack item, int damage) {
        return typed(item, Damageable.class, "Cet objet n'a pas de durabilité", meta -> {
            if (damage < 0) {
                throw new EditException("Les dégâts ne peuvent pas être négatifs");
            }
            meta.setDamage(damage);
            return "Dégâts réglés sur " + damage;
        });
    }

    static Edit maxDamage(ItemStack item, Integer maxDamage) {
        return typed(item, Damageable.class, "Cet objet n'a pas de durabilité", meta -> {
            if (maxDamage != null && maxDamage < 1) {
                throw new EditException("La durabilité maximale doit être au moins 1");
            }
            if (maxDamage != null && item.getMaxStackSize() > 1) {
                meta.setMaxStackSize(1);
            }
            meta.setMaxDamage(maxDamage);
            return maxDamage == null ? "Durabilité maximale par défaut" : "Durabilité maximale réglée sur " + maxDamage;
        });
    }

    static Edit unbreakable(ItemStack item, String state) {
        return meta(item, meta -> {
            boolean value = ItemLookup.state(state, meta.isUnbreakable());
            meta.setUnbreakable(value);
            return value ? "Objet incassable" : "Objet cassable";
        });
    }

    static Edit glider(ItemStack item, String state) {
        return meta(item, meta -> {
            boolean value = ItemLookup.state(state, meta.isGlider());
            meta.setGlider(value);
            return value ? "Planeur activé" : "Planeur désactivé";
        });
    }

    static Edit fireResistant(ItemStack item, String state) {
        return meta(item, meta -> {
            boolean value = ItemLookup.state(state, meta.hasDamageResistant());
            meta.setFireResistant(value);
            return value ? "Résiste au feu" : "Brûle normalement";
        });
    }

    static Edit hideTooltip(ItemStack item, String state) {
        return meta(item, meta -> {
            boolean value = ItemLookup.state(state, meta.isHideTooltip());
            meta.setHideTooltip(value);
            return value ? "Infobulle masquée" : "Infobulle affichée";
        });
    }

    static Edit glint(ItemStack item, Boolean glint) {
        return meta(item, meta -> {
            meta.setEnchantmentGlintOverride(glint);
            return glint == null ? "Brillance par défaut" : glint ? "Brillance forcée" : "Brillance retirée";
        });
    }

    static Edit rarity(ItemStack item, ItemRarity rarity) {
        return meta(item, meta -> {
            meta.setRarity(rarity);
            return "Rareté : " + ItemNaming.rarity(rarity);
        });
    }

    static Edit customModelData(ItemStack item, Integer value) {
        return meta(item, meta -> {
            meta.setCustomModelData(value);
            return value == null ? "Modèle personnalisé retiré" : "Modèle personnalisé réglé sur " + value;
        });
    }

    static Edit itemModel(ItemStack item, NamespacedKey model) {
        return meta(item, meta -> {
            meta.setItemModel(model);
            return model == null ? "Modèle d'objet par défaut" : "Modèle d'objet : " + model;
        });
    }

    static Edit tooltipStyle(ItemStack item, NamespacedKey style) {
        return meta(item, meta -> {
            meta.setTooltipStyle(style);
            return style == null ? "Style d'infobulle par défaut" : "Style d'infobulle : " + style;
        });
    }

    static Edit enchantable(ItemStack item, Integer value) {
        return meta(item, meta -> {
            if (value != null && value < 1) {
                throw new EditException("L'enchantabilité doit être au moins 1");
            }
            meta.setEnchantable(value);
            return value == null ? "Enchantabilité par défaut" : "Enchantabilité réglée sur " + value;
        });
    }

    static Edit type(ItemStack item, Material material) {
        if (material == null || material.isAir() || !material.isItem()) {
            return Edit.failed("Ce matériau ne peut pas être tenu en main");
        }
        ItemStack changed = item.withType(material);
        return Edit.done(changed, "Matériau changé en " + material.getKey().getKey());
    }

    static Edit color(ItemStack item, Color color) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(color);
        } else if (meta instanceof PotionMeta potion) {
            potion.setColor(color);
        } else {
            return Edit.failed("Seuls le cuir teint et les potions ont une couleur");
        }
        item.setItemMeta(meta);
        return Edit.done(item, color == null ? "Couleur par défaut"
                : "Couleur réglée sur #" + String.format(Locale.ROOT, "%06X", color.asRGB()));
    }

    static Edit skullProfile(ItemStack item, PlayerProfile profile) {
        return typed(item, SkullMeta.class, "Seules les têtes de joueur peuvent changer de skin", meta -> {
            meta.setPlayerProfile(profile);
            return "Tête de " + (profile.getName() == null ? "joueur" : profile.getName());
        });
    }

    static Edit skullTexture(ItemStack item, String texture) {
        return typed(item, SkullMeta.class, "Seules les têtes de joueur peuvent changer de texture", meta -> {
            ItemStack textured = ItemBuilder.skull().texture(texture.trim()).build();
            if (!(textured.getItemMeta() instanceof SkullMeta source) || source.getPlayerProfile() == null) {
                throw new EditException("Texture invalide");
            }
            meta.setPlayerProfile(source.getPlayerProfile());
            return "Texture de tête appliquée";
        });
    }

    static Edit potionEffect(ItemStack item, PotionEffectType effect, int seconds, int level) {
        return typed(item, PotionMeta.class, "Seules les potions et flèches à effet peuvent porter des effets", meta -> {
            if (seconds < 1) {
                throw new EditException("La durée doit être d'au moins 1 seconde");
            }
            if (level < 1 || level > ItemLookup.MAX_ENCHANT_LEVEL) {
                throw new EditException("Le niveau doit être entre 1 et " + ItemLookup.MAX_ENCHANT_LEVEL);
            }
            meta.addCustomEffect(new PotionEffect(effect, seconds * TICKS_PER_SECOND, level - 1), true);
            return "Effet " + ItemLookup.shortKey(effect) + " " + level + " pendant " + seconds + "s";
        });
    }

    static Edit removePotionEffect(ItemStack item, PotionEffectType effect) {
        return typed(item, PotionMeta.class, "Cet objet n'est pas une potion", meta -> {
            if (!meta.removeCustomEffect(effect)) {
                throw new EditException("Cette potion n'a pas l'effet " + ItemLookup.shortKey(effect));
            }
            return "Effet " + ItemLookup.shortKey(effect) + " retiré";
        });
    }

    static Edit potionType(ItemStack item, PotionType type) {
        return typed(item, PotionMeta.class, "Cet objet n'est pas une potion", meta -> {
            meta.setBasePotionType(type);
            return type == null ? "Potion de base retirée" : "Potion de base : " + ItemLookup.shortKey(type);
        });
    }

    static Edit clearPotion(ItemStack item) {
        return typed(item, PotionMeta.class, "Cet objet n'est pas une potion", meta -> {
            meta.clearCustomEffects();
            return "Effets de potion retirés";
        });
    }

    static Edit trim(ItemStack item, TrimMaterial material, TrimPattern pattern) {
        return typed(item, ArmorMeta.class, "Seules les pièces d'armure acceptent une garniture", meta -> {
            meta.setTrim(material == null || pattern == null ? null : new ArmorTrim(material, pattern));
            return material == null ? "Garniture retirée"
                    : "Garniture " + ItemLookup.shortKey(pattern) + " en " + ItemLookup.shortKey(material);
        });
    }

    static Edit bookTitle(ItemStack item, String raw) {
        return typed(item, BookMeta.class, "Seuls les livres écrits ont un titre", meta -> {
            meta.title(ItemText.parse(raw));
            return "Titre du livre modifié";
        });
    }

    static Edit bookAuthor(ItemStack item, String raw) {
        return typed(item, BookMeta.class, "Seuls les livres écrits ont un auteur", meta -> {
            meta.author(ItemText.parse(raw));
            return "Auteur du livre modifié";
        });
    }

    static Edit repairCost(ItemStack item, int cost) {
        return typed(item, Repairable.class, "Cet objet ne passe pas à l'enclume", meta -> {
            if (cost < 0) {
                throw new EditException("Le coût de réparation ne peut pas être négatif");
            }
            meta.setRepairCost(cost);
            return "Coût de réparation réglé sur " + cost;
        });
    }
}
