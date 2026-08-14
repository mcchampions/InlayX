package me.qscbm.inlayx.socket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.event.GemExtractEvent;
import me.qscbm.inlayx.api.event.GemExtractedEvent;
import me.qscbm.inlayx.api.event.GemSocketEvent;
import me.qscbm.inlayx.api.event.GemSocketedEvent;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemItemFactory;
import me.qscbm.inlayx.gem.GemTemplate;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

/**
 * 装备宝石槽位机制
 */
public class SocketService {
    static final String SOCKETED_GEMS_KEY = "socketed_gems";

    private final InlayX plugin;
    private final GemItemFactory itemFactory;
    private final Map<String, Gem> gems;

    public SocketService(InlayX plugin, GemItemFactory itemFactory, Map<String, Gem> gems) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.gems = gems;
    }

    // ==================== PDC 槽位列表读写 ====================
    public List<SocketSlot> getSocketSlots(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return List.of();
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null ? List.of() : readSlots(meta);
    }

    private List<SocketSlot> readSlots(ItemMeta meta) {
        List<SocketSlot> slots = new ArrayList<>();
        if (meta == null) {
            return slots;
        }
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return slots;
        }
        Map<Integer, SocketSlot> filledByStart = readFilledSlots(meta);
        String header = plugin.getConfigManager().getSocketHeader();
        int headerIdx = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).equals(header)) {
                headerIdx = i;
                break;
            }
        }
        if (headerIdx < 0) {
            return new ArrayList<>(filledByStart.values());
        }

        int areaSize = lore.size() - headerIdx - 1;
        int offset = 0;
        int index = 0;
        Set<Integer> usedStarts = new HashSet<>();
        while (offset < areaSize) {
            SocketSlot filled = filledByStart.get(offset);
            if (filled != null) {
                if (filled.getEnd() < areaSize) {
                    usedStarts.add(offset);
                    filled.setIndex(index);
                    slots.add(filled);
                    offset = filled.getEnd() + 1;
                    index++;
                    continue;
                }
                break;
            }
            String line = lore.get(headerIdx + 1 + offset);
            GemType type = matchEmptyType(line);
            // 未知行 -> 宝石区域结束
            if (type == null) {
                break;
            }
            slots.add(new SocketSlot(index, type.getId(), null, offset, offset));
            offset++;
            index++;
        }
        List<SocketSlot> orphans = new ArrayList<>();
        for (Map.Entry<Integer, SocketSlot> entry : filledByStart.entrySet()) {
            if (!usedStarts.contains(entry.getKey())) {
                orphans.add(entry.getValue());
            }
        }
        if (!orphans.isEmpty()) {
            plugin.getLogger().warning("装备宝石 PDC 与 lore 不一致, 已保留 " + orphans.size() + " 条已镶嵌记录, 下次操作时会重新对齐");
            orphans.sort(Comparator.comparingInt(SocketSlot::getStart));
            slots.addAll(orphans);
        }
        return slots;
    }

    private Map<Integer, SocketSlot> readFilledSlots(ItemMeta meta) {
        Map<Integer, SocketSlot> filled = new HashMap<>();
        String stored = meta.getPersistentDataContainer().get(socketedGemsKey(), PersistentDataType.STRING);
        if (stored == null || stored.isEmpty()) {
            return filled;
        }
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String gemId = o.isNull("gemId") ? null : o.getString("gemId");
                String type = o.isNull("type") ? null : o.getString("type");
                int start = o.optInt("start", 0);
                int end = o.optInt("end", 0);
                filled.put(start, new SocketSlot(-1, type, gemId, start, end));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("解析宝石槽位数据失败: " + e.getMessage());
        }
        return filled;
    }

    private void writeSlots(ItemMeta meta, List<SocketSlot> slots) {
        JSONArray arr = new JSONArray();
        for (SocketSlot slot : slots) {
            if (slot.getGemId() == null) {
                continue;
            }
            JSONObject o = new JSONObject();
            o.put("gemId", slot.getGemId());
            if (slot.getType() != null) {
                o.put("type", slot.getType());
            }
            o.put("start", slot.getStart());
            o.put("end", slot.getEnd());
            arr.put(o);
        }
        if (arr.isEmpty()) {
            meta.getPersistentDataContainer().remove(socketedGemsKey());
        } else {
            meta.getPersistentDataContainer().set(socketedGemsKey(), PersistentDataType.STRING, arr.toString());
        }
    }

    // ==================== 物品判断 ====================

    public boolean hasSocketLore(ItemStack item) {
        return !getSocketSlots(item).isEmpty();
    }

    public boolean hasSocketedGems(ItemStack item) {
        return getSocketSlots(item).stream().anyMatch(slot -> slot.getGemId() != null);
    }

    public List<String> getSocketedGems(ItemStack item) {
        return getSocketSlots(item).stream()
                .map(SocketSlot::getGemId)
                .filter(Objects::nonNull)
                .toList();
    }

    public int getSocketCount(ItemStack item) {
        return getSocketSlots(item).size();
    }

    public int getSocketCount(ItemMeta meta) {
        return meta == null ? 0 : readSlots(meta).size();
    }

    public boolean loreHasEmptySocket(ItemMeta meta) {
        return meta != null && readSlots(meta).stream().anyMatch(slot -> slot.getGemId() == null);
    }

    public boolean canSocketGem(ItemStack item) {
        return loreHasEmptySocket(item == null ? null : item.getItemMeta());
    }

    public boolean canSocketGem(ItemMeta meta) {
        return loreHasEmptySocket(meta);
    }

    public boolean canSocketGemType(ItemStack equipment, String gemId) {
        Gem gem = gems.get(gemId);
        if (gem == null || equipment == null) {
            return false;
        }
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return false;
        }
        return findEmptySlotIndex(readSlots(meta), gem.getType()) >= 0;
    }

    // ==================== 槽位操作 ====================

    public ItemStack addSlotToItem(ItemStack item, int sockets, GemType socketType) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<SocketSlot> slots = readSlots(meta);
        int maxSockets = plugin.getConfigManager().getMaxSockets();
        sockets = Math.min(sockets, Math.max(0, maxSockets - slots.size()));
        if (sockets <= 0) {
            return item;
        }
        int nextIndex = slots.isEmpty() ? 0 : slots.get(slots.size() - 1).getIndex() + 1;
        for (int i = 0; i < sockets; i++) {
            slots.add(new SocketSlot(nextIndex + i, socketType.getId(), null, 0, 0));
        }
        return rebuildSocketArea(item, slots);
    }

    public ItemStack removeSlotFromItem(ItemStack item, int sockets, GemType socketType) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<SocketSlot> slots = readSlots(meta);
        List<SocketSlot> keep = new ArrayList<>(slots.size());
        int removed = 0;
        for (SocketSlot slot : slots) {
            if (removed < sockets
                    && slot.getGemId() == null
                    && socketType.getId().equals(slot.getType())) {
                removed++;
            } else {
                keep.add(slot);
            }
        }
        if (removed == 0) {
            return item;
        }
        return rebuildSocketArea(item, keep);
    }

    public SocketResult socketGem(ItemStack equipment, ItemStack gemItem) {
        return socketGem(null, equipment, gemItem);
    }

    public SocketResult socketGem(@Nullable Player actor, ItemStack equipment, ItemStack gemItem) {
        if (equipment == null
                || equipment.getType() == Material.AIR
                || gemItem == null
                || gemItem.getType() == Material.AIR) {
            return SocketResult.failure(SocketResult.Status.INVALID_INPUT);
        }
        ItemMeta equipMeta = equipment.getItemMeta();
        ItemMeta gemMeta = gemItem.getItemMeta();
        if (equipMeta == null || gemMeta == null) {
            return SocketResult.failure(SocketResult.Status.INVALID_INPUT);
        }

        String gemId = itemFactory.getGemId(gemMeta);
        if (gemId == null) {
            return SocketResult.failure(SocketResult.Status.NOT_A_GEM);
        }
        Gem gem = gems.get(gemId);
        if (gem == null) {
            return SocketResult.failure(SocketResult.Status.UNKNOWN_GEM);
        }
        return socketWithEvent(actor, equipment, gem, gemItem, true);
    }

    public SocketResult addGem(ItemStack equipment, String gemId) {
        return addGem(null, equipment, gemId);
    }

    public SocketResult addGem(@Nullable Player actor, ItemStack equipment, String gemId) {
        if (equipment == null || equipment.getType() == Material.AIR || gemId == null || gemId.isEmpty()) {
            return SocketResult.failure(SocketResult.Status.INVALID_INPUT);
        }
        Gem gem = gems.get(gemId);
        if (gem == null) {
            return SocketResult.failure(SocketResult.Status.UNKNOWN_GEM);
        }
        return socketWithEvent(actor, equipment, gem, null, false);
    }

    private SocketResult socketWithEvent(
            @Nullable Player actor, ItemStack equipment, Gem gem, @Nullable ItemStack gemItem, boolean roll) {
        GemSocketEvent event = new GemSocketEvent(actor, equipment, gem, gemItem);
        if (!event.callEvent()) {
            return SocketResult.failure(SocketResult.Status.CANCELLED);
        }
        SocketResult result = socketGem(equipment, gem, roll);
        if (result.isSuccess()) {
            new GemSocketedEvent(actor, result.getItem(), gem).callEvent();
        }
        return result;
    }

    private SocketResult socketGem(ItemStack equipment, Gem gem, boolean roll) {
        ItemMeta equipMeta = equipment.getItemMeta();
        if (equipMeta == null) {
            return SocketResult.failure(SocketResult.Status.INVALID_INPUT);
        }
        List<SocketSlot> slots = readSlots(equipMeta);
        if (slots.size() > plugin.getConfigManager().getMaxSockets()) {
            return SocketResult.failure(SocketResult.Status.OVER_CAP_LIMIT);
        }
        if (slots.isEmpty() || slots.stream().noneMatch(slot -> slot.getGemId() == null)) {
            return SocketResult.failure(SocketResult.Status.NO_SOCKET);
        }
        int idx = findEmptySlotIndex(slots, gem.getType());
        if (idx < 0) {
            return SocketResult.failure(SocketResult.Status.TYPE_MISMATCH);
        }
        if (roll && ThreadLocalRandom.current().nextDouble() >= gem.getSocketSuccessRate()) {
            return SocketResult.failure(SocketResult.Status.FAILED);
        }
        slots.get(idx).setGemId(gem.getId());
        ItemStack result = rebuildSocketArea(equipment, slots);
        return SocketResult.success(result);
    }

    public ExtractResult extractGem(ItemStack item, String gemId) {
        return extractGem(null, item, gemId);
    }

    public ExtractResult extractGem(@Nullable Player actor, ItemStack item, String gemId) {
        if (!hasSocketedGem(item, gemId)) {
            return ExtractResult.notFound();
        }
        GemExtractEvent event = new GemExtractEvent(actor, item, gemId);
        if (!event.callEvent()) {
            return ExtractResult.cancelled(gemId);
        }
        if (!removeSocketedGem(item, gemId)) {
            return ExtractResult.notFound();
        }
        boolean success = ThreadLocalRandom.current().nextDouble()
                < plugin.getConfigManager().getExtractSuccessRate();
        new GemExtractedEvent(actor, item, gemId, success).callEvent();
        return success ? ExtractResult.success(gemId) : ExtractResult.failed(gemId);
    }

    private boolean hasSocketedGem(ItemStack item, String gemId) {
        if (item == null || item.getType() == Material.AIR || gemId == null || gemId.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        for (SocketSlot slot : readSlots(meta)) {
            if (gemId.equals(slot.getGemId())) {
                return true;
            }
        }
        return false;
    }

    public boolean removeGem(ItemStack item, String gemId) {
        return removeSocketedGem(item, gemId);
    }

    private boolean removeSocketedGem(ItemStack item, String gemId) {
        if (item == null || item.getType() == Material.AIR || gemId == null || gemId.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        List<SocketSlot> slots = readSlots(meta);
        int idx = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (gemId.equals(slots.get(i).getGemId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return false;
        }
        SocketSlot slot = slots.get(idx);
        if (slot.getType() == null) {
            Gem gem = gems.get(gemId);
            if (gem != null) {
                slot.setType(gem.getType().getId());
            }
        }
        slot.setGemId(null);
        rebuildSocketArea(item, slots);
        return true;
    }

    // ==================== 区域重建 ====================

    private ItemStack rebuildSocketArea(ItemStack item, List<SocketSlot> slots) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        String header = plugin.getConfigManager().getSocketHeader();
        int headerIdx = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).equals(header)) {
                headerIdx = i;
                break;
            }
        }
        if (slots.isEmpty()) {
            if (headerIdx >= 0) {
                while (lore.size() > headerIdx) {
                    lore.remove(lore.size() - 1);
                }
            }
            writeSlots(meta, slots);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
        if (headerIdx < 0) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(header);
            headerIdx = lore.size() - 1;
        }
        while (lore.size() > headerIdx + 1) {
            lore.remove(lore.size() - 1);
        }
        int offset = 0;
        for (int i = 0; i < slots.size(); i++) {
            SocketSlot slot = slots.get(i);
            slot.setIndex(i);
            List<String> block = renderBlock(slot);
            slot.setStart(offset);
            slot.setEnd(offset + block.size() - 1);
            lore.addAll(block);
            offset += block.size();
        }
        writeSlots(meta, slots);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> renderBlock(SocketSlot slot) {
        if (slot.getGemId() == null) {
            return List.of(renderEmptySlot(slot.getType()));
        }
        Gem gem = gems.get(slot.getGemId());
        if (gem == null) {
            return List.of(renderEmptySlot(slot.getType()));
        }
        return renderFilledBlock(gem);
    }

    private String renderEmptySlot(String typeId) {
        String line =
                TextUtils.translateAlternateColorCodes(plugin.getConfigManager().getSocketEmptyPattern());
        GemType type = plugin.getConfigManager().getGemType(typeId);
        if (type == null) {
            return line.replace("{gemTypeColor}", ChatColor.GRAY.toString())
                    .replace("{gemTypeName}", "未知")
                    .replace("{gemTypeId}", typeId == null ? "unknown" : typeId);
        }
        return renderEmptySlot(type);
    }

    private String renderEmptySlot(GemType type) {
        return TextUtils.translateAlternateColorCodes(plugin.getConfigManager().getSocketEmptyPattern())
                .replace("{gemTypeColor}", type.getColor().toString())
                .replace("{gemTypeName}", type.getName())
                .replace("{gemTypeId}", type.getId());
    }

    private GemType matchEmptyType(String line) {
        for (GemType type : plugin.getConfigManager().getGemTypes().values()) {
            if (renderEmptySlot(type).equals(line)) {
                return type;
            }
        }
        return null;
    }

    private List<String> renderFilledBlock(Gem gem) {
        List<String> lines = new ArrayList<>();
        for (String line : plugin.getConfigManager().getSocketFilledPattern()) {
            if ("{attributeLores}".equals(line)) {
                for (String attr : gem.getAttributeLore()) {
                    lines.add(TextUtils.translateAlternateColorCodes(GemTemplate.parse(
                            plugin.getConfigManager().getSocketAttributeLorePattern(), gem, "{attributeLore}", attr)));
                }
            } else {
                lines.add(TextUtils.translateAlternateColorCodes(
                        GemTemplate.parse(line, gem, "{gemDisplayName}", gem.getDisplayName())));
            }
        }
        return lines;
    }

    // ==================== 内部工具方法 ====================

    private NamespacedKey socketedGemsKey() {
        return new NamespacedKey(this.plugin, SOCKETED_GEMS_KEY);
    }

    private int findEmptySlotIndex(List<SocketSlot> slots, GemType type) {
        for (int i = 0; i < slots.size(); i++) {
            SocketSlot slot = slots.get(i);
            if (slot.getGemId() == null && type.getId().equals(slot.getType())) {
                return i;
            }
        }
        return -1;
    }
}
