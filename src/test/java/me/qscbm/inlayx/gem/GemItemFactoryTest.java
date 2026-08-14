package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.junit.jupiter.api.Test;

class GemItemFactoryTest extends InlayXTestBase {

    private Gem registerItemGem(String id, Material material) {
        Gem gem = new Gem(id, "物品宝石", plugin.getConfigManager().getGemType("ATTACK"), 1, material);
        gem.setDisplayName("物品宝石");
        gem.setLore(List.of());
        plugin.getGemManager().registerGem(gem);
        return gem;
    }

    @Test
    void identifiesAndCreatesGemItems() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack item = plugin.getGemManager().createGemItem("t1");
        assertNotNull(item);
        assertTrue(plugin.getGemManager().isGem(item));
        assertEquals("t1", plugin.getGemManager().getGemId(item));
        assertEquals("测试宝石", item.getItemMeta().getDisplayName());
        assertFalse(plugin.getGemManager().isGem(new ItemStack(Material.EMERALD)));
        assertNull(plugin.getGemManager().createGemItem("missing"));
    }

    @Test
    void appliesEnchantmentsFlagsAttributesAndModelData() {
        Gem gem = registerItemGem("item_1", Material.DIAMOND_SWORD);
        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        gem.addEnchantment(sharpness, 5);
        gem.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        gem.addAttribute(Attribute.ATTACK_DAMAGE, 10, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        gem.setCustomModelData(42);

        ItemStack item = plugin.getGemManager().createGemItem("item_1");
        ItemMeta meta = item.getItemMeta();
        assertEquals(5, meta.getEnchantLevel(sharpness));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
        assertEquals(42, meta.getCustomModelData());
        assertFalse(meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE).isEmpty());
    }

    @Test
    void appliesLeatherColorAndPotionEffects() {
        Gem leather = registerItemGem("item_leather", Material.LEATHER_HELMET);
        leather.setLeatherColor(Color.fromRGB(255, 0, 0));
        ItemStack leatherItem = plugin.getGemManager().createGemItem("item_leather");
        assertTrue(leatherItem.getItemMeta() instanceof LeatherArmorMeta);
        assertEquals(Color.fromRGB(255, 0, 0), ((LeatherArmorMeta) leatherItem.getItemMeta()).getColor());

        Gem potion = registerItemGem("item_potion", Material.LINGERING_POTION);
        potion.addPotionEffect(Registry.MOB_EFFECT.get(NamespacedKey.minecraft("speed")), 200, 1, false, true, false);
        ItemStack potionItem = plugin.getGemManager().createGemItem("item_potion");
        assertTrue(potionItem.getItemMeta() instanceof PotionMeta);
        PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
        assertEquals(1, potionMeta.getCustomEffects().size());
        assertEquals(
                "speed", potionMeta.getCustomEffects().get(0).getType().getKey().getKey());
    }

    @Test
    void appliesDurabilityFormats() {
        Gem damageGem = registerItemGem("item_damage", Material.DIAMOND_SWORD);
        damageGem.setDurability(new Gem.DurabilityEntry(Gem.DurabilityMode.DAMAGE, 20));
        ItemStack damageItem = plugin.getGemManager().createGemItem("item_damage");
        assertEquals(20, ((Damageable) damageItem.getItemMeta()).getDamage());

        Gem remainingGem = registerItemGem("item_remaining", Material.DIAMOND_SWORD);
        remainingGem.setDurability(new Gem.DurabilityEntry(Gem.DurabilityMode.REMAINING, 20));
        ItemStack remainingItem = plugin.getGemManager().createGemItem("item_remaining");
        int maxDamage = Material.DIAMOND_SWORD.getMaxDurability();
        assertEquals(maxDamage - 20, ((Damageable) remainingItem.getItemMeta()).getDamage());

        Gem percentGem = registerItemGem("item_percent", Material.DIAMOND_SWORD);
        percentGem.setDurability(new Gem.DurabilityEntry(Gem.DurabilityMode.PERCENT, 50));
        ItemStack percentItem = plugin.getGemManager().createGemItem("item_percent");
        int percentMax = Material.DIAMOND_SWORD.getMaxDurability();
        assertEquals(
                percentMax - (int) Math.round(percentMax * 0.5), ((Damageable) percentItem.getItemMeta()).getDamage());
    }
}
