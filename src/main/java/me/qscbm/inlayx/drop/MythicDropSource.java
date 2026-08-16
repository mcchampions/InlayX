package me.qscbm.inlayx.drop;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import me.qscbm.inlayx.api.DropCandidate;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.integration.MythicMobsBridge;
import org.bukkit.entity.LivingEntity;

final class MythicDropSource implements DropSource {
    private final MythicMobsBridge bridge;

    MythicDropSource(MythicMobsBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NonNull String id() {
        return DropSource.MYTHIC;
    }

    @Override
    public @NonNull Map<String, Object> defaultSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("enable", true);
        settings.put("priority", 1);
        settings.put("chance", 0.1);
        settings.put("min_mob_level", 1);
        settings.put("per_level_rate", 0.001);
        return settings;
    }

    @Override
    public void handleEntityDeath(@NonNull DropSourceContext context) {
        LivingEntity entity = context.getEntity();
        if (!bridge.isMythicMob(entity)) {
            return;
        }
        int mobLevel = bridge.getMobLevel(entity);
        DropCandidate selected = DropMath.selectWeighted(context.getCandidates(), candidate -> {
            if (mobLevel < candidate.settings().getInt("min_mob_level", 1)) {
                return 0;
            }
            double chance = candidate.settings().getDouble("chance", 0);
            double perLevelRate = candidate.settings().getDouble("per_level_rate", 0);
            return chance + mobLevel * perLevelRate;
        });
        if (selected != null) {
            context.select(selected);
        }
    }
}
