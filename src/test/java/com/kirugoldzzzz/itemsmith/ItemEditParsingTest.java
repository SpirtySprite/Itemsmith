package com.kirugoldzzzz.itemsmith;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEditParsingTest {

    private static List<Component> lines(String... texts) {
        List<Component> lore = new ArrayList<>();
        for (String text : texts) {
            lore.add(Component.text(text));
        }
        return lore;
    }

    private static List<String> plain(List<Component> lore) {
        return lore.stream().map(ItemText::plain).toList();
    }

    @Test
    @DisplayName("Les lignes de description s'ajoutent, se remplacent et s'insèrent au bon endroit")
    void loreAddSetInsert() {
        List<Component> lore = LoreEdit.add(null, Component.text("un"));
        lore = LoreEdit.add(lore, Component.text("trois"));
        lore = LoreEdit.insert(lore, 2, Component.text("deux"));
        lore = LoreEdit.insert(lore, 4, Component.text("quatre"));
        lore = LoreEdit.set(lore, 1, Component.text("UN"));
        assertEquals(List.of("UN", "deux", "trois", "quatre"), plain(lore));
    }

    @Test
    @DisplayName("Les lignes se retirent et se déplacent sans toucher à l'original")
    void loreRemoveMove() {
        List<Component> original = lines("a", "b", "c", "d");
        assertEquals(List.of("a", "c", "d"), plain(LoreEdit.remove(original, 2)));
        assertEquals(List.of("b", "c", "a", "d"), plain(LoreEdit.move(original, 1, 3)));
        assertEquals(List.of("d", "a", "b", "c"), plain(LoreEdit.move(original, 4, 1)));
        assertEquals(List.of("a", "b", "c", "d"), plain(original));
    }

    @Test
    @DisplayName("Une ligne hors limites est refusée avec un message clair")
    void loreBounds() {
        EditException missing = assertThrows(EditException.class, () -> LoreEdit.set(lines("a"), 3, Component.empty()));
        assertTrue(missing.getMessage().contains("entre 1 et 1"));
        assertThrows(EditException.class, () -> LoreEdit.remove(List.of(), 1));
        assertThrows(EditException.class, () -> LoreEdit.insert(lines("a"), 3, Component.empty()));
        List<Component> full = new ArrayList<>();
        for (int index = 0; index < LoreEdit.MAX_LINES; index++) {
            full.add(Component.empty());
        }
        assertThrows(EditException.class, () -> LoreEdit.add(full, Component.empty()));
    }

    @Test
    @DisplayName("Les états acceptent on, off et leurs équivalents, et basculent sans valeur")
    void states() {
        assertTrue(ItemLookup.state("on", false));
        assertTrue(ItemLookup.state("oui", false));
        assertFalse(ItemLookup.state("off", true));
        assertFalse(ItemLookup.state("non", true));
        assertTrue(ItemLookup.state(null, false));
        assertFalse(ItemLookup.state(" ", true));
        assertThrows(EditException.class, () -> ItemLookup.state("peut-être", true));
        assertTrue(ItemLookup.reset("RESET"));
        assertTrue(ItemLookup.reset("défaut"));
        assertFalse(ItemLookup.reset("12"));
    }

    @Test
    @DisplayName("Les nombres sont bornés et acceptent la virgule française")
    void numbers() {
        assertEquals(64, ItemLookup.integer("64", 1, 99, "La quantité"));
        EditException tooBig = assertThrows(EditException.class, () -> ItemLookup.integer("300", 1, 255, "Le niveau"));
        assertEquals("Le niveau doit être entre 1 et 255", tooBig.getMessage());
        assertThrows(EditException.class, () -> ItemLookup.integer("abc", 1, 255, "Le niveau"));
        assertEquals(2.5D, ItemLookup.decimal("2,5", "La valeur"));
        assertEquals(-1.0D, ItemLookup.decimal("-1", "La valeur"));
        assertThrows(EditException.class, () -> ItemLookup.decimal("NaN", "La valeur"));
    }

    @Test
    @DisplayName("Les couleurs se lisent en hexadécimal ou par nom")
    void colors() {
        assertEquals(Color.fromRGB(0x12AB34), ItemLookup.color("#12ab34"));
        assertEquals(Color.fromRGB(0x12AB34), ItemLookup.color("12AB34"));
        assertEquals(Color.fromRGB(0xFF5555), ItemLookup.color("red"));
        assertThrows(EditException.class, () -> ItemLookup.color("#123"));
        assertThrows(EditException.class, () -> ItemLookup.color("arc-en-ciel"));
    }

    @Test
    @DisplayName("Opérations, raretés et masquages acceptent des alias lisibles")
    void aliases() {
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, ItemLookup.operation(null));
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, ItemLookup.operation("+"));
        assertEquals(AttributeModifier.Operation.ADD_SCALAR, ItemLookup.operation("percent"));
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, ItemLookup.operation("multiplier"));
        assertThrows(EditException.class, () -> ItemLookup.operation("divide"));
        assertEquals(ItemRarity.EPIC, ItemLookup.rarity("épique"));
        assertEquals(ItemRarity.UNCOMMON, ItemLookup.rarity("peu-commun"));
        assertThrows(EditException.class, () -> ItemLookup.rarity("mythique"));
        assertEquals(ItemFlag.HIDE_ENCHANTS, ItemLookup.flag("enchants"));
        assertEquals(ItemFlag.HIDE_ARMOR_TRIM, ItemLookup.flag("hide-armor-trim"));
        assertThrows(EditException.class, () -> ItemLookup.flag("everything"));
    }

    @Test
    @DisplayName("La rareté du menu tourne dans l'ordre puis revient par défaut")
    void rarityCycle() {
        assertEquals(ItemRarity.COMMON, ItemEditMenu.nextRarity(null));
        assertEquals(ItemRarity.UNCOMMON, ItemEditMenu.nextRarity(ItemRarity.COMMON));
        assertEquals(ItemRarity.EPIC, ItemEditMenu.nextRarity(ItemRarity.RARE));
        assertNull(ItemEditMenu.nextRarity(ItemRarity.EPIC));
    }

    @Test
    @DisplayName("Le texte d'une commande est recollé et les montants sont lisibles")
    void textHelpers() {
        assertEquals("<gold>Épée du Nexus", ItemText.join(new String[]{"name", "<gold>Épée", "du", "Nexus"}, 1));
        assertEquals("", ItemText.join(new String[]{"name"}, 1));
        assertEquals("Épée", ItemText.plain(ItemText.parse("&6Épée")));
        assertEquals("5", ItemAttributeMenu.format(5.0D));
        assertEquals("0.25", ItemAttributeMenu.format(0.25D));
        assertTrue(ItemLookup.key("nexus:epee").toString().equals("nexus:epee"));
        assertThrows(EditException.class, () -> ItemLookup.key("Pas Une Clé"));
    }

    @Test
    @DisplayName("Chaque message utilisé par l'éditeur existe dans messages.yml")
    void messageKeysExist() throws IOException {
        YamlConfiguration messages;
        try (Reader reader = Files.newBufferedReader(Path.of("src", "main", "resources", "messages.yml"),
                StandardCharsets.UTF_8)) {
            messages = YamlConfiguration.loadConfiguration(reader);
        }
        Pattern literal = Pattern.compile("\"(item-edit\\.[a-z-]+)\"");
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of("src", "main", "java", "com", "kirugoldzzzz", "itemsmith"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Matcher matcher = literal.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        assertTrue(keys.size() >= 7, keys.toString());
        for (String key : keys) {
            assertTrue(messages.contains(key), key);
        }
    }
}
