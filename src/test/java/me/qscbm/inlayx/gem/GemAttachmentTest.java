package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Collection;
import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class GemAttachmentTest extends InlayXTestBase {
    private Gem registerAttachmentGem(String id, String configText) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(configText));
        ConfigurationSection section = yaml.getConfigurationSection(id);
        plugin.getGemManager().getLoader().parseAndRegister(id, section);
        return plugin.getGemManager().getGem(id);
    }

    @Test
    void parsesAttachmentEnchantmentsAndAttributes() {
        Gem gem = registerAttachmentGem("attach_gem", """
                attach_gem:
                  name: "附带宝石"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    enchantments:
                      - "SHARPNESS:3"
                    attributes:
                      - "ATTACK_DAMAGE:10:0:OFF_HAND"
                      - "LUCK:5:1"
                """);
        GemAttachment attachment = gem.getGemAttachment();
        assertNotNull(attachment);

        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        assertEquals(3, attachment.getEnchantments().get(sharpness));

        assertEquals(2, attachment.getAttributes().size());
        AttributeEntry attackDamage = attachment.getAttributes().getFirst();
        assertEquals(Attribute.ATTACK_DAMAGE, attackDamage.attribute());
        assertEquals(10, attackDamage.amount());
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, attackDamage.operation());
        assertEquals(EquipmentSlot.OFF_HAND, attackDamage.slot());

        AttributeEntry luck = attachment.getAttributes().get(1);
        assertEquals(Attribute.LUCK, luck.attribute());
        assertEquals(5, luck.amount());
        assertEquals(AttributeModifier.Operation.ADD_SCALAR, luck.operation());
        assertNull(luck.slot());
    }

    @Test
    void parsesAttachmentStripsGenericPrefixAndMapsMainHandAlias() {
        Gem gem = registerAttachmentGem("alias_gem", """
                alias_gem:
                  name: "别名宝石"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    attributes:
                      - "GENERIC_ATTACK_SPEED:2:0:MAINHAND"
                """);
        GemAttachment attachment = gem.getGemAttachment();
        assertNotNull(attachment);
        AttributeEntry entry = attachment.getAttributes().getFirst();
        assertEquals(Attribute.ATTACK_SPEED, entry.attribute());
        assertEquals(EquipmentSlot.HAND, entry.slot());
    }

    @Test
    void parseSkipsMalformedAndInvalidEntries() {
        Gem gem = registerAttachmentGem("bad_attach", """
                bad_attach:
                  name: "坏附带"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    enchantments:
                      - "格式错误"
                      - "NOT_A_REAL_ENCHANT:1"
                      - "SHARPNESS:abc"
                      - "SHARPNESS:2"
                    attributes:
                      - "ATTACK_DAMAGE:10"
                      - "NOT_AN_ATTRIBUTE:1:0"
                      - "ATTACK_DAMAGE:not_a_number:0"
                      - "LUCK:3:0"
                """);
        GemAttachment attachment = gem.getGemAttachment();
        assertNotNull(attachment);
        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        assertEquals(2, attachment.getEnchantments().get(sharpness));
        assertEquals(1, attachment.getAttributes().size());
        assertEquals(Attribute.LUCK, attachment.getAttributes().getFirst().attribute());
    }

    @Test
    void socketToItemMetaAddsModifiersAndEnchantments() {
        Gem gem = registerAttachmentGem("socket_gem", """
                socket_gem:
                  name: "镶嵌宝石"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    enchantments:
                      - "SHARPNESS:3"
                    attributes:
                      - "ATTACK_DAMAGE:10:0:OFF_HAND"
                """);
        GemAttachment attachment = gem.getGemAttachment();

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        ItemMeta result = attachment.socketToItemMeta(meta);

        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        assertEquals(3, result.getEnchantLevel(sharpness));

        Collection<AttributeModifier> modifiers = result.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
        assertNotNull(modifiers);
        assertTrue(modifiers.stream().anyMatch(m -> m.getKey().getKey().equals("attr_socket_gem_off_hand_1")));
    }

    @Test
    void extractToItemMetaRemovesModifiersAndEnchantments() {
        Gem gem = registerAttachmentGem("extract_gem", """
                extract_gem:
                  name: "提取宝石"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    enchantments:
                      - "SHARPNESS:3"
                    attributes:
                      - "ATTACK_DAMAGE:10:0:OFF_HAND"
                """);
        GemAttachment attachment = gem.getGemAttachment();

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta = attachment.socketToItemMeta(meta);

        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        assertEquals(3, meta.getEnchantLevel(sharpness));
        assertFalse(meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE).stream()
                .noneMatch(m -> m.getKey().getKey().startsWith("attr_extract_gem_")));

        meta = attachment.extractToItemMeta(meta);
        assertEquals(0, meta.getEnchantLevel(sharpness));
        Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
        assertTrue(modifiers == null
                || modifiers.stream().noneMatch(m -> m.getKey().getKey().startsWith("attr_extract_gem_")));
    }

    @Test
    void socketToItemStackLeavesEmptyAttachmentUnchanged() {
        Gem gem = registerAttachmentGem("empty_attach", """
                empty_attach:
                  name: "空附带"
                  type: UTILITY
                  level: 1
                  material: BONE
                  attachment:
                    enchantments: []
                    attributes: []
                """);
        GemAttachment attachment = gem.getGemAttachment();
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack result = attachment.socketToItemStack(sword);
        assertSame(sword, result);
    }
}
