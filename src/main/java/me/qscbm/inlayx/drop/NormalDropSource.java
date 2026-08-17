package me.qscbm.inlayx.drop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import me.qscbm.inlayx.api.DropCandidate;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

final class NormalDropSource implements DropSource {
    private static final List<String> DEFAULT_ALLOW_ENTITIES = defaultAllowEntities();

    @Override
    public @NonNull String id() {
        return DropSource.NORMAL;
    }

    @Override
    public @NonNull Map<String, Object> defaultSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("enable", true);
        settings.put("priority", 0);
        settings.put("chance", 0.1);
        settings.put("allow_entities", DEFAULT_ALLOW_ENTITIES);
        return settings;
    }

    @Override
    public void handleEntityDeath(@NonNull DropSourceContext context) {
        LivingEntity entity = context.getEntity();
        List<DropCandidate> allowed = new ArrayList<>();
        for (DropCandidate candidate : context.getCandidates()) {
            Set<String> allowEntities = candidate.settings().getStringList("allow_entities").stream()
                    .map(name -> name.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (!allowEntities.contains(entity.getType().name())) {
                continue;
            }
            allowed.add(candidate);
        }
        DropCandidate selected = DropMath.selectWeighted(
                allowed, candidate -> candidate.settings().getDouble("chance", 0));
        if (selected != null) {
            context.select(selected);
        }
    }

    private static List<String> defaultAllowEntities() {
        return java.util.Arrays.stream(EntityType.values())
                .filter(type -> type.getEntityClass() != null && Mob.class.isAssignableFrom(type.getEntityClass()))
                .map(Enum::name)
                .sorted()
                .toList();
    }
}
