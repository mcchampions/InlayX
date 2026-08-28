package me.qscbm.inlayx.talisman;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.JSONObject;

/**
 * 保护符效果
 */
public class TalismanEffect {
    /**
     * 成功率加成效果.
     */
    public record BonusEntry(String id, double bonus, int uses) {}

    /**
     * 防碎裂效果.
     */
    public record PreventEntry(String id, int uses) {}

    /**
     * 宝石当前持有的全部保护效果.
     */
    public record State(BonusEntry bonus, PreventEntry prevent) {
        public static State empty() {
            return new State(null, null);
        }

        public boolean isEmpty() {
            return bonus == null && prevent == null;
        }
    }

    private final NamespacedKey key;

    public TalismanEffect(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

    // ==================== 读取 / 写入 ====================

    public State read(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return State.empty();
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null ? State.empty() : read(meta);
    }

    public State read(ItemMeta meta) {
        if (meta == null) {
            return State.empty();
        }
        String stored = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (stored == null || stored.isEmpty()) {
            return State.empty();
        }
        try {
            JSONObject root = new JSONObject(stored);
            BonusEntry bonus = null;
            PreventEntry prevent = null;
            if (root.has("bonus") && !root.isNull("bonus")) {
                JSONObject b = root.getJSONObject("bonus");
                bonus = new BonusEntry(b.getString("id"), b.getDouble("bonus"), b.getInt("uses"));
            }
            if (root.has("prevent") && !root.isNull("prevent")) {
                JSONObject p = root.getJSONObject("prevent");
                prevent = new PreventEntry(p.getString("id"), p.getInt("uses"));
            }
            return new State(bonus, prevent);
        } catch (Exception e) {
            return State.empty();
        }
    }

    public void write(ItemStack item, State state) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        write(meta, state);
        item.setItemMeta(meta);
    }

    public void write(ItemMeta meta, State state) {
        if (meta == null) {
            return;
        }
        if (state == null || state.isEmpty()) {
            meta.getPersistentDataContainer().remove(key);
            return;
        }
        JSONObject root = new JSONObject();
        if (state.bonus() != null) {
            JSONObject b = new JSONObject();
            b.put("id", state.bonus().id());
            b.put("bonus", state.bonus().bonus());
            b.put("uses", state.bonus().uses());
            root.put("bonus", b);
        }
        if (state.prevent() != null) {
            JSONObject p = new JSONObject();
            p.put("id", state.prevent().id());
            p.put("uses", state.prevent().uses());
            root.put("prevent", p);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, root.toString());
    }

    // ==================== 消耗 ====================

    /**
     * 消耗 1 次成功率加成, 次数归零后效果移除.
     */
    public static State consumeBonusUse(State state) {
        if (state == null || state.bonus() == null) {
            return state;
        }
        BonusEntry bonus = state.bonus();
        if (bonus.uses() <= 1) {
            return new State(null, state.prevent());
        }
        return new State(new BonusEntry(bonus.id(), bonus.bonus(), bonus.uses() - 1), state.prevent());
    }

    /**
     * 消耗 1 次防碎裂, 次数归零后效果移除.
     */
    public static State consumePreventUse(State state) {
        if (state == null || state.prevent() == null) {
            return state;
        }
        PreventEntry prevent = state.prevent();
        if (prevent.uses() <= 1) {
            return new State(state.bonus(), null);
        }
        return new State(state.bonus(), new PreventEntry(prevent.id(), prevent.uses() - 1));
    }
}
