package me.qscbm.inlayx.drop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.DropCandidate;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.api.event.GemDropEvent;
import me.qscbm.inlayx.config.ConfigManager.DropSourceMode;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 掉落协调器.
 * <p>
 * 负责合并配置, 按 priority 排序来源, 调用来源处理掉落并执行 FIRST/ALL 模式.
 */
public final class DropCoordinator {
    private final InlayX plugin;

    public DropCoordinator(@NonNull InlayX plugin) {
        this.plugin = plugin;
    }

    public void onEntityDeath(@NonNull EntityDeathEvent event) {
        if (!plugin.getConfigManager().isDropSystemEnabled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        List<SourceGroup> groups = buildGroups();
        for (SourceGroup group : groups) {
            DropCandidate selected;
            try {
                DropSourceContext context = new DropSourceContext(entity, killer, group.candidates);
                group.source.handleEntityDeath(context);
                selected = context.getSelected();
            } catch (Exception e) {
                plugin.getLogger().warning("掉落来源 " + group.source.id() + " 处理实体死亡时出错: " + e.getMessage());
                continue;
            }
            if (selected == null) {
                continue;
            }
            GemDropEvent dropEvent =
                    new GemDropEvent(entity, killer, group.source, selected.gem(), selected.settings());
            if (!dropEvent.callEvent() || dropEvent.getGem() == null) {
                continue;
            }
            ItemStack item =
                    plugin.getGemManager().createGemItem(dropEvent.getGem().getId());
            if (item == null) {
                continue;
            }
            event.getDrops().add(item);
            if (plugin.getConfigManager().getDropSourceMode() == DropSourceMode.FIRST) {
                return;
            }
        }
    }

    private List<SourceGroup> buildGroups() {
        List<DropSourceRegistry.RegisteredSource> entries =
                plugin.getDropSourceRegistry().getEntries();
        List<SourceGroup> groups = new ArrayList<>();
        for (int order = 0; order < entries.size(); order++) {
            DropSourceRegistry.RegisteredSource entry = entries.get(order);
            List<DropCandidate> candidates = new ArrayList<>();
            for (Gem gem :
                    plugin.getGemManager().getDropCandidates(entry.source().id())) {
                ConfigurationSection settings = plugin.getDropSourceConfigManager()
                        .mergeSettings(
                                entry.source().id(),
                                gem.getDropSourceSettings().get(entry.source().id()));
                if (!settings.getBoolean("enable", true)) {
                    continue;
                }
                candidates.add(new DropCandidate(gem, settings));
            }
            if (candidates.isEmpty()) {
                continue;
            }
            candidates.sort(Comparator.comparingInt(
                            (DropCandidate candidate) -> candidate.settings().getInt("priority", 0))
                    .reversed());
            int priority = candidates.getFirst().settings().getInt("priority", 0);
            groups.add(new SourceGroup(entry.source(), candidates, priority, order));
        }
        groups.sort(Comparator.comparingInt(SourceGroup::priority).reversed().thenComparingInt(SourceGroup::order));
        return groups;
    }

    private record SourceGroup(DropSource source, List<DropCandidate> candidates, int priority, int order) {}
}
