package me.qscbm.inlayx.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.event.GemExtractEvent;
import me.qscbm.inlayx.api.event.GemExtractedEvent;
import me.qscbm.inlayx.api.event.GemSocketEvent;
import me.qscbm.inlayx.api.event.GemSocketedEvent;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemItemFactory;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemTemplate;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.socket.ExtractResult;
import me.qscbm.inlayx.socket.SocketResult;
import me.qscbm.inlayx.socket.SocketSlot;
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
    private final AtomicReference<GemManager.GemRegistry> registry;

    public SocketService(InlayX plugin, GemItemFactory itemFactory, AtomicReference<GemManager.GemRegistry> registry) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.registry = registry;
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
        List<SocketSlot> filledSlots = readFilledSlots(meta);
        int headerIdx = findIndex(lore, plugin.getConfigManager().getSocketHeader(), 0);
        if (headerIdx >= 0) {
            return readSlotsWithHeader(lore, headerIdx, filledSlots);
        }
        if (filledSlots.isEmpty()) {
            return slots;
        }
        HeaderlessRecovery recovery = recoverHeaderlessArea(lore, filledSlots);
        return recovery == null ? new ArrayList<>(filledSlots) : recovery.slots;
    }

    private List<SocketSlot> readSlotsWithHeader(List<String> lore, int headerIdx, List<SocketSlot> filledSlots) {
        int footerIdx = findIndex(lore, plugin.getConfigManager().getSocketFooter(), headerIdx + 1);
        int areaSize = footerIdx >= 0
                ? footerIdx - headerIdx - 1
                : recoverAreaSizeAfterLastFilled(lore, headerIdx, filledSlots);
        return parseBoundedArea(lore, headerIdx, areaSize, filledSlots);
    }

    private List<SocketSlot> parseBoundedArea(
            List<String> lore, int headerIdx, int areaSize, List<SocketSlot> filledSlots) {
        List<SocketSlot> slots = new ArrayList<>();
        Map<Integer, SocketSlot> filledByStart = new HashMap<>();
        for (SocketSlot filled : filledSlots) {
            filledByStart.put(filled.getStart(), filled);
        }
        Set<Integer> usedStarts = new HashSet<>();
        int offset = 0;
        int index = 0;
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
            GemType type = matchEmptyType(lore.get(headerIdx + 1 + offset));
            if (type == null) {
                break;
            }
            slots.add(new SocketSlot(index, type.id(), null, offset, offset));
            offset++;
            index++;
        }
        List<SocketSlot> orphans = new ArrayList<>();
        for (SocketSlot filled : filledSlots) {
            if (!usedStarts.contains(filled.getStart())) {
                orphans.add(filled);
            }
        }
        if (!orphans.isEmpty()) {
            plugin.getLogger().warning("装备宝石 PDC 与 lore 不一致, 已保留 " + orphans.size() + " 条已镶嵌记录, 下次操作时会重新对齐");
            slots.addAll(orphans);
        }
        return slots;
    }

    private int recoverAreaSizeAfterLastFilled(List<String> lore, int headerIdx, List<SocketSlot> filledSlots) {
        int maxEnd = -1;
        for (SocketSlot filled : filledSlots) {
            maxEnd = Math.max(maxEnd, filled.getEnd());
        }
        int offset = Math.clamp(maxEnd + 1, 0, lore.size() - headerIdx - 1);
        while (offset < lore.size() - headerIdx - 1) {
            if (matchEmptyType(lore.get(headerIdx + 1 + offset)) == null) {
                break;
            }
            offset++;
        }
        return offset;
    }

    private HeaderlessRecovery recoverHeaderlessArea(List<String> lore, List<SocketSlot> filledSlots) {
        String footer = plugin.getConfigManager().getSocketFooter();
        for (SocketSlot candidate : filledSlots) {
            List<String> block = renderedFilledBlock(candidate);
            if (block.isEmpty()) {
                continue;
            }
            int anchor = findBlock(lore, 0, block);
            if (anchor < 0) {
                continue;
            }

            List<SocketSlot> leadingEmpties = new ArrayList<>();
            int up = anchor - 1;
            while (up >= 0) {
                GemType type = matchEmptyType(lore.get(up));
                if (type == null) {
                    break;
                }
                leadingEmpties.add(new SocketSlot(-1, type.id(), null, up, up));
                up--;
            }
            int headerInsertIndex = up + 1;

            Set<SocketSlot> used = new HashSet<>();
            used.add(candidate);
            List<SocketSlot> trailingSlots = new ArrayList<>();
            trailingSlots.add(candidate);
            int cursor = anchor + block.size();
            int areaEndIndex = cursor;
            boolean footerFound = false;
            while (cursor < lore.size()) {
                if (lore.get(cursor).equals(footer)) {
                    footerFound = true;
                    areaEndIndex = cursor;
                    break;
                }
                GemType type = matchEmptyType(lore.get(cursor));
                if (type != null) {
                    trailingSlots.add(new SocketSlot(-1, type.id(), null, cursor, cursor));
                    cursor++;
                    areaEndIndex = cursor;
                    continue;
                }
                SocketSlot next = findMatchingFilledAt(lore, cursor, filledSlots, used, candidate.getStart());
                if (next == null) {
                    areaEndIndex = cursor;
                    break;
                }
                used.add(next);
                trailingSlots.add(next);
                cursor += renderedFilledBlock(next).size();
                areaEndIndex = cursor;
            }

            List<SocketSlot> slots = new ArrayList<>();
            for (int i = leadingEmpties.size() - 1; i >= 0; i--) {
                slots.add(leadingEmpties.get(i));
            }
            slots.addAll(trailingSlots);
            for (SocketSlot filled : filledSlots) {
                if (!used.contains(filled)) {
                    slots.add(filled);
                }
            }
            for (int i = 0; i < slots.size(); i++) {
                slots.get(i).setIndex(i);
            }
            return new HeaderlessRecovery(headerInsertIndex, areaEndIndex, footerFound, slots);
        }
        return null;
    }

    private SocketSlot findMatchingFilledAt(
            List<String> lore, int pos, List<SocketSlot> filledSlots, Set<SocketSlot> used, int minStart) {
        for (SocketSlot candidate : filledSlots) {
            if (used.contains(candidate) || candidate.getStart() < minStart) {
                continue;
            }
            List<String> block = renderedFilledBlock(candidate);
            if (block.isEmpty() || pos + block.size() > lore.size()) {
                continue;
            }
            if (matchesAt(lore, pos, block)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> renderedFilledBlock(SocketSlot slot) {
        if (slot == null || slot.getGemId() == null) {
            return List.of();
        }
        Gem gem = registry.get().gems().get(slot.getGemId());
        return gem == null ? List.of() : renderFilledBlock(gem);
    }

    private int findBlock(List<String> lore, int from, List<String> block) {
        if (block.isEmpty()) {
            return -1;
        }
        for (int i = from; i + block.size() <= lore.size(); i++) {
            if (matchesAt(lore, i, block)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesAt(List<String> lore, int pos, List<String> block) {
        for (int i = 0; i < block.size(); i++) {
            if (!block.get(i).equals(lore.get(pos + i))) {
                return false;
            }
        }
        return true;
    }

    private List<SocketSlot> readFilledSlots(ItemMeta meta) {
        List<SocketSlot> filled = new ArrayList<>();
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
                filled.add(new SocketSlot(-1, type, gemId, start, end));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("解析宝石槽位数据失败: " + e.getMessage());
        }
        filled.sort(Comparator.comparingInt(SocketSlot::getStart).thenComparingInt(SocketSlot::getEnd));
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
        Gem gem = registry.get().gems().get(gemId);
        if (gem == null || equipment == null) {
            return false;
        }
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (!gem.canSocketTo(equipment.getType())) {
            return false;
        }
        return findEmptySlotIndex(readSlots(meta), gem.getType()) >= 0;
    }

    // ==================== 槽位操作 ====================

    public ItemStack addSlotToItem(ItemStack item, int sockets, GemType socketType) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<SocketSlot> slots = readSlots(meta);
        int maxSockets = plugin.getConfigManager().getMaxSockets();
        sockets = Math.clamp(maxSockets - slots.size(), 0, sockets);
        if (sockets <= 0) {
            return item;
        }
        int nextIndex = slots.isEmpty() ? 0 : slots.getLast().getIndex() + 1;
        for (int i = 0; i < sockets; i++) {
            slots.add(new SocketSlot(nextIndex + i, socketType.id(), null, 0, 0));
        }
        return rebuildSocketArea(item, slots);
    }

    public ItemStack removeSlotFromItem(ItemStack item, int sockets, GemType socketType) {
        if (item == null || item.getType() == Material.AIR) {
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
            if (removed < sockets && slot.getGemId() == null && socketType.id().equals(slot.getType())) {
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
        Gem gem = registry.get().gems().get(gemId);
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
        Gem gem = registry.get().gems().get(gemId);
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
        if (!gem.canSocketTo(equipment.getType())) {
            return SocketResult.failure(SocketResult.Status.MATERIAL_MISMATCH);
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
            Gem gem = registry.get().gems().get(gemId);
            if (gem != null) {
                slot.setType(gem.getType().id());
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
        List<SocketSlot> oldFilledSlots = readFilledSlots(meta);
        String header = plugin.getConfigManager().getSocketHeader();
        String footer = plugin.getConfigManager().getSocketFooter();
        int headerIdx = findIndex(lore, header, 0);
        int footerIdx = headerIdx >= 0 ? findIndex(lore, footer, headerIdx + 1) : -1;
        List<String> newLore = new ArrayList<>();

        if (slots.isEmpty()) {
            if (headerIdx >= 0) {
                newLore.addAll(lore.subList(0, headerIdx));
                int suffixStart =
                        footerIdx >= 0 ? footerIdx + 1 : recoverFooterInsertionIndex(lore, headerIdx, oldFilledSlots);
                newLore.addAll(lore.subList(suffixStart, lore.size()));
            } else {
                newLore.addAll(lore);
            }
            writeSlots(meta, slots);
            meta.setLore(newLore);
            item.setItemMeta(meta);
            return item;
        }

        if (headerIdx >= 0) {
            newLore.addAll(lore.subList(0, headerIdx + 1));
            appendRenderedSlots(newLore, slots);
            newLore.add(footer);
            int suffixStart =
                    footerIdx >= 0 ? footerIdx + 1 : recoverFooterInsertionIndex(lore, headerIdx, oldFilledSlots);
            newLore.addAll(lore.subList(suffixStart, lore.size()));
        } else {
            HeaderlessRecovery recovery = recoverHeaderlessArea(lore, oldFilledSlots);
            if (recovery == null) {
                newLore.addAll(lore);
                if (!newLore.isEmpty()) {
                    newLore.add("");
                }
                newLore.add(header);
                appendRenderedSlots(newLore, slots);
                newLore.add(footer);
            } else {
                newLore.addAll(lore.subList(0, recovery.headerInsertIndex));
                newLore.add(header);
                appendRenderedSlots(newLore, slots);
                newLore.add(footer);
                int suffixStart = recovery.footerFound ? recovery.areaEndIndex + 1 : recovery.areaEndIndex;
                newLore.addAll(lore.subList(suffixStart, lore.size()));
            }
        }
        writeSlots(meta, slots);
        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    private int recoverFooterInsertionIndex(List<String> lore, int headerIdx, List<SocketSlot> filledSlots) {
        int maxEnd = -1;
        for (SocketSlot filled : filledSlots) {
            maxEnd = Math.max(maxEnd, filled.getEnd());
        }
        int cursor = Math.min(headerIdx + 1 + Math.max(maxEnd + 1, 0), lore.size());
        while (cursor < lore.size()) {
            if (matchEmptyType(lore.get(cursor)) == null) {
                break;
            }
            cursor++;
        }
        if (cursor < lore.size()
                && lore.get(cursor).equals(plugin.getConfigManager().getSocketFooter())) {
            return cursor + 1;
        }
        return cursor;
    }

    private void appendRenderedSlots(List<String> lore, List<SocketSlot> slots) {
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
    }

    private int findIndex(List<String> lore, String target, int from) {
        for (int i = from; i < lore.size(); i++) {
            if (target.equals(lore.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<String> renderBlock(SocketSlot slot) {
        if (slot.getGemId() == null) {
            return List.of(renderEmptySlot(slot.getType()));
        }
        Gem gem = registry.get().gems().get(slot.getGemId());
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
                .replace("{gemTypeColor}", type.color().toString())
                .replace("{gemTypeName}", type.name())
                .replace("{gemTypeId}", type.id());
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

    private record HeaderlessRecovery(
            int headerInsertIndex, int areaEndIndex, boolean footerFound, List<SocketSlot> slots) {}

    // ==================== 内部工具方法 ====================

    private NamespacedKey socketedGemsKey() {
        return new NamespacedKey(this.plugin, SOCKETED_GEMS_KEY);
    }

    private int findEmptySlotIndex(List<SocketSlot> slots, GemType type) {
        for (int i = 0; i < slots.size(); i++) {
            SocketSlot slot = slots.get(i);
            if (slot.getGemId() == null && type.id().equals(slot.getType())) {
                return i;
            }
        }
        return -1;
    }
}
