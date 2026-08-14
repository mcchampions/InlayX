package me.qscbm.inlayx.gem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gui.ExtractGuiFactory;
import me.qscbm.inlayx.gui.SocketGuiFactory;
import me.qscbm.inlayx.socket.ExtractResult;
import me.qscbm.inlayx.socket.SocketResult;
import me.qscbm.inlayx.socket.SocketService;
import me.qscbm.inlayx.socket.SocketSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 宝石管理
 */
public class GemManager {
    private final InlayX plugin;

    private final Map<String, Gem> gems;

    @Getter
    private final GemLoader loader;

    @Getter
    private final GemItemFactory itemFactory;

    @Getter
    private final SocketService socketService;

    @Getter
    private final SocketGuiFactory guiFactory;

    @Getter
    private final ExtractGuiFactory extractGuiFactory;

    private final Map<String, List<Gem>> dropIndex;

    public GemManager(InlayX plugin) {
        this.plugin = plugin;
        this.gems = new HashMap<>();
        this.dropIndex = new HashMap<>();
        this.loader = new GemLoader(plugin, gems);
        this.itemFactory = new GemItemFactory(plugin, gems);
        this.socketService = new SocketService(plugin, itemFactory, gems);
        this.guiFactory = new SocketGuiFactory(plugin);
        this.extractGuiFactory = new ExtractGuiFactory(plugin, this);
        loadGems();
    }

    public void loadGems() {
        loader.loadAll();
        rebuildDropIndex();
    }

    private void rebuildDropIndex() {
        dropIndex.clear();
        for (Gem gem : gems.values()) {
            for (String source : gem.getDropSources()) {
                dropIndex.computeIfAbsent(source, k -> new ArrayList<>()).add(gem);
            }
        }
    }

    // ==================== 查询 ====================

    public Map<String, Gem> getGems() {
        return Collections.unmodifiableMap(this.gems);
    }

    public Collection<Gem> getAllGems() {
        return Collections.unmodifiableCollection(this.gems.values());
    }

    public Gem getGem(String id) {
        return this.gems.get(id);
    }

    /**
     * 按掉落来源与怪物等级随机选取一颗掉落的宝石.
     * <p>
     * 各宝石的最终掉落率 = drop.chance + mobLevel * drop.per_level_rate (限制在 [0, 1]).
     * 总掉落率 = min(各宝石最终掉落率之和, 1.0), 命中后按最终掉落率加权选择一颗宝石.
     */
    public Gem getDropGem(String source, int mobLevel) {
        List<Gem> candidates = dropIndex.get(source);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<WeightedGem> eligible = new ArrayList<>();
        double totalChance = 0.0;
        for (Gem gem : candidates) {
            if (mobLevel < gem.getMinMobLevel()) {
                continue;
            }
            double chance = Math.clamp(gem.getDropChance() + mobLevel * gem.getLevelBonus(), 0.0, 1.0);
            if (chance <= 0.0) {
                continue;
            }
            eligible.add(new WeightedGem(gem, chance));
            totalChance += chance;
        }
        if (eligible.isEmpty()) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble();
        double target = roll * Math.max(1.0, totalChance);
        if (target >= totalChance) {
            return null;
        }
        double cumulative = 0.0;
        for (WeightedGem weighted : eligible) {
            cumulative += weighted.chance;
            if (target < cumulative) {
                return weighted.gem;
            }
        }
        return eligible.get(eligible.size() - 1).gem;
    }

    /**
     * 注册一个宝石(供外部 API 使用), 重复 ID 时覆盖旧定义, 并刷新掉落索引.
     */
    public void registerGem(Gem gem) {
        if (gem == null) {
            throw new IllegalArgumentException("gem 不能为 null");
        }
        gems.put(gem.getId(), gem);
        rebuildDropIndex();
    }

    /**
     * 注销一个宝石, 返回被移除的宝石定义, 并刷新掉落索引.
     */
    public Gem unregisterGem(String gemId) {
        Gem removed = gems.remove(gemId);
        if (removed != null) {
            rebuildDropIndex();
        }
        return removed;
    }

    public boolean isGem(ItemStack item) {
        return itemFactory.isGem(item);
    }

    public String getGemId(ItemStack item) {
        return itemFactory.getGemId(item);
    }

    public ItemStack createGemItem(String gemId) {
        return itemFactory.createGemItem(gemId);
    }

    public boolean loreHasEmptySocket(ItemMeta meta) {
        return socketService.loreHasEmptySocket(meta);
    }

    public boolean canSocketGem(ItemStack item) {
        return socketService.canSocketGem(item);
    }

    public boolean canSocketGem(ItemMeta meta) {
        return socketService.canSocketGem(meta);
    }

    public boolean canSocketGemType(ItemStack equipment, String gemId) {
        return socketService.canSocketGemType(equipment, gemId);
    }

    public boolean hasSocketedGems(ItemStack item) {
        return socketService.hasSocketedGems(item);
    }

    public List<String> getSocketedGems(ItemStack item) {
        return socketService.getSocketedGems(item);
    }

    public List<SocketSlot> getSocketSlots(ItemStack item) {
        return socketService.getSocketSlots(item);
    }

    public int getSocketCount(ItemStack item) {
        return socketService.getSocketCount(item);
    }

    public int getSocketCount(ItemMeta meta) {
        return socketService.getSocketCount(meta);
    }

    public boolean hasSocketLore(ItemStack item) {
        return socketService.hasSocketLore(item);
    }

    public ItemStack addSlotToItem(ItemStack item, int sockets, GemType socketType) {
        return socketService.addSlotToItem(item, sockets, socketType);
    }

    public ItemStack removeSlotFromItem(ItemStack item, int sockets, GemType socketType) {
        return socketService.removeSlotFromItem(item, sockets, socketType);
    }

    public SocketResult socketGem(ItemStack equipment, ItemStack gemItem) {
        return socketService.socketGem(equipment, gemItem);
    }

    public SocketResult addGem(ItemStack equipment, String gemId) {
        return socketService.addGem(equipment, gemId);
    }

    public ExtractResult extractGem(ItemStack item, String gemId) {
        return socketService.extractGem(item, gemId);
    }

    public boolean removeGem(ItemStack item, String gemId) {
        return socketService.removeGem(item, gemId);
    }

    public Inventory createSocketGUI() {
        return guiFactory.createSocketGUI();
    }

    private record WeightedGem(Gem gem, double chance) {}
}
