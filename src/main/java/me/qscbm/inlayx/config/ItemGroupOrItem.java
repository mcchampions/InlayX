package me.qscbm.inlayx.config;

import java.util.List;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Material;

@Data
public class ItemGroupOrItem {
    private boolean itemGroup;

    private List<Material> items;

    private List<ItemGroupOrItem> subItemGroups;

    private Material item;

    private String id;

    private String name;

    private ItemGroupOrItem(
            boolean isItemGroup,
            List<Material> items,
            Material item,
            String id,
            String name,
            List<ItemGroupOrItem> subItemGroups) {
        this.itemGroup = isItemGroup;
        this.id = id;
        this.name = name;

        this.items = items;
        this.item = item;
        this.subItemGroups = subItemGroups;
    }

    public static ItemGroupOrItem createItemGroup(
            @NonNull String id,
            @NonNull String name,
            @NonNull List<Material> materials,
            @NonNull List<ItemGroupOrItem> subItemGroups) {
        for (ItemGroupOrItem sub : subItemGroups) {
            if (!sub.isItemGroup()) {
                throw new IllegalArgumentException("Sub item groups cannot contain items");
            }
        }
        return new ItemGroupOrItem(true, materials, null, id, name, subItemGroups);
    }

    public static ItemGroupOrItem createItem(@NonNull String id, @NonNull String name, @NonNull Material material) {
        return new ItemGroupOrItem(false, null, material, id, name, null);
    }

    public boolean containsItem(Material material) {
        if (itemGroup) {
            if (items.contains(material)) {
                return true;
            }
            for (ItemGroupOrItem subItemGroup : subItemGroups) {
                if (subItemGroup.containsItem(material)) {
                    return true;
                }
            }
            return false;
        } else {
            return item == material;
        }
    }
}
