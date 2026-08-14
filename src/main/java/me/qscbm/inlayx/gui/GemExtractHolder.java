package me.qscbm.inlayx.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 提取宝石 GUI 的 InventoryHolder
 */
@Getter
@Setter
public final class GemExtractHolder implements InventoryHolder {
    private int page;

    public GemExtractHolder(int page) {
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
