package me.qscbm.inlayx.gem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import me.qscbm.inlayx.InlayX;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 宝石附加属性
 * 将应用到被镶嵌物品中
 */
public class GemAttachment {
    private final Map<Enchantment, Integer> enchantments;

    private final List<AttributeEntry> attributes;

    private final Gem gem;

    public GemAttachment(
            @NonNull Map<Enchantment, Integer> enchantments,
            @NonNull List<AttributeEntry> attributes,
            @NonNull Gem gem) {
        this.enchantments = enchantments;
        this.attributes = attributes;
        this.gem = gem;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    public List<AttributeEntry> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void addAttribute(
            Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        attributes.add(new AttributeEntry(attribute, amount, operation, slot));
    }

    public ItemStack socketToItemStack(@NonNull ItemStack itemStack) {
        if (enchantments.isEmpty() && attributes.isEmpty()) {
            return itemStack;
        }
        boolean hasItemMeta = itemStack.hasItemMeta();
        ItemMeta itemMeta;
        if (hasItemMeta) {
            itemMeta = itemStack.getItemMeta();
        } else {
            itemMeta = InlayX.INSTANCE.getServer().getItemFactory().getItemMeta(itemStack.getType());
        }
        if (itemMeta == null) {
            InlayX.INSTANCE.getLogger().warning("无法获取物品元数据:" + itemStack.getType());
            return itemStack;
        }
        itemStack.setItemMeta(socketToItemMeta(itemMeta));
        return itemStack;
    }

    public ItemMeta socketToItemMeta(@NonNull ItemMeta meta) {
        for (AttributeEntry attribute : getAttributes()) {
            EquipmentSlot slot = attribute.slot();
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute.attribute());
            if (modifiers == null) {
                modifiers = new ArrayList<>();
            }
            int index = 1;
            for (AttributeModifier existModifier : modifiers) {
                String key = existModifier.getKey().getKey();
                boolean matched = key.matches(
                        ("attr_" + gem.getId() + (slot == null ? "" : "_" + slot.name()) + "_[0-9]+").toLowerCase());
                if (matched) {
                    index++;
                }
            }
            AttributeModifier modifier = getModifier(attribute, slot, index);
            meta.addAttributeModifier(attribute.attribute(), modifier);
        }
        meta = InlayX.INSTANCE
                .getAttachmentHandlerConfigManager()
                .applyEnchantmentConfigWhenSocket(meta, enchantments, gem);
        return meta;
    }

    private AttributeModifier getModifier(AttributeEntry attribute, EquipmentSlot slot, int index) {
        NamespacedKey modifierKey = new NamespacedKey(
                InlayX.INSTANCE,
                ("attr_" + gem.getId() + (slot == null ? "" : "_" + slot.name()) + "_" + index).toLowerCase());
        return slot == null
                ? new AttributeModifier(modifierKey, attribute.amount(), attribute.operation())
                : new AttributeModifier(modifierKey, attribute.amount(), attribute.operation(), slot.getGroup());
    }

    public ItemStack extractToItemStack(@NonNull ItemStack itemStack) {
        if (enchantments.isEmpty() && attributes.isEmpty()) {
            return itemStack;
        }
        boolean hasItemMeta = itemStack.hasItemMeta();
        ItemMeta itemMeta;
        if (hasItemMeta) {
            itemMeta = itemStack.getItemMeta();
        } else {
            itemMeta = InlayX.INSTANCE.getServer().getItemFactory().getItemMeta(itemStack.getType());
        }
        if (itemMeta == null) {
            InlayX.INSTANCE.getLogger().warning("无法获取物品元数据:" + itemStack.getType());
            return itemStack;
        }
        itemStack.setItemMeta(socketToItemMeta(itemMeta));
        return itemStack;
    }

    public ItemMeta extractToItemMeta(@NonNull ItemMeta meta) {
        for (AttributeEntry attribute : getAttributes()) {
            EquipmentSlot slot = attribute.slot();

            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute.attribute());
            if (modifiers == null) {
                modifiers = new ArrayList<>();
            }
            int index = 0;
            for (AttributeModifier existModifier : modifiers) {
                String key = existModifier.getKey().getKey();
                boolean matched = key.matches(
                        ("attr_" + gem.getId() + (slot == null ? "" : "_" + slot.name()) + "_[0-9]+").toLowerCase());
                if (matched) {
                    index++;
                }
            }
            AttributeModifier modifier = getModifier(attribute, slot, index);
            meta.removeAttributeModifier(attribute.attribute(), modifier);
        }
        meta = InlayX.INSTANCE
                .getAttachmentHandlerConfigManager()
                .applyEnchantmentConfigWhenExtract(meta, enchantments, gem);
        return meta;
    }

    public static GemAttachment parse(
            @NonNull ConfigurationSection configuration, @NonNull Gem gem, @NonNull GemLoader gemLoader) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        for (String entry : configuration.getStringList("enchantments")) {
            int colon = entry.lastIndexOf(':');
            if (colon <= 0 || colon == entry.length() - 1) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("宝石 " + gem.getId() + " 的 attachement.enchantments 格式错误: " + entry + ", 应使用 附魔名:等级");
                continue;
            }
            String enchantName = entry.substring(0, colon);
            int level;
            try {
                level = Integer.parseInt(entry.substring(colon + 1));
            } catch (NumberFormatException e) {
                InlayX.INSTANCE.getLogger().warning("宝石 " + gem.getId() + " 的 attachement.enchantments 等级无效: " + entry);
                continue;
            }
            Enchantment enchantment = gemLoader.parseEnchantment(enchantName);
            if (enchantment == null) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("宝石 " + gem.getId() + " 的 attachement.enchantments 含有无效附魔: " + enchantName);
                continue;
            }
            enchantments.put(enchantment, level);
        }

        List<AttributeEntry> attributes = new ArrayList<>();
        for (String entry : configuration.getStringList("attributes")) {
            String[] parts = entry.split(":");
            if (parts.length < 3 || parts.length > 4) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("宝石 " + gem.getId() + " 的 attachement.attributes 格式错误: " + entry
                                + ", 应使用 属性名:数字:模式[:生效位置]");
                continue;
            }
            Attribute attribute = gemLoader.parseAttribute(parts[0]);
            if (attribute == null) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("宝石 " + gem.getId() + " 的 attachement.attributes 含有无效属性: " + parts[0]);
                continue;
            }
            double amount;
            try {
                amount = Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("宝石 " + gem.getId() + " 的 attachement.attributes 数值无效: " + parts[1]);
                continue;
            }
            AttributeModifier.Operation operation = gemLoader.parseOperation(parts[2]);
            EquipmentSlot slot = parts.length == 4 ? gemLoader.parseEquipmentSlot(parts[3]) : null;
            attributes.add(new AttributeEntry(attribute, amount, operation, slot));
        }
        return new GemAttachment(enchantments, attributes, gem);
    }
}
