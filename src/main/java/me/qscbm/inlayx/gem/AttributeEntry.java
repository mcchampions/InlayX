package me.qscbm.inlayx.gem;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 一个原版属性修饰符的配置值
 */
public record AttributeEntry(
        Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {}
