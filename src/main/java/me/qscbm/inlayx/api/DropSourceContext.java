package me.qscbm.inlayx.api;

import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 掉落来源的实体死亡上下文.
 */
public final class DropSourceContext {
    private final @NonNull LivingEntity entity;
    private final @NonNull Player killer;
    private final @NonNull List<DropCandidate> candidates;
    private @Nullable DropCandidate selected;

    public DropSourceContext(
            @NonNull LivingEntity entity, @NonNull Player killer, @NonNull List<DropCandidate> candidates) {
        this.entity = entity;
        this.killer = killer;
        this.candidates = Collections.unmodifiableList(List.copyOf(candidates));
    }

    /**
     * 触发掉落的实体.
     */
    public @NonNull LivingEntity getEntity() {
        return entity;
    }

    /**
     * 杀死实体的玩家.
     */
    public @NonNull Player getKiller() {
        return killer;
    }

    /**
     * 当前来源的候选宝石, 已按优先级从高到低排列.
     */
    public @NonNull List<DropCandidate> getCandidates() {
        return candidates;
    }

    /**
     * 来源选择要掉落的宝石.
     */
    public void select(@NonNull DropCandidate candidate) {
        if (!candidates.contains(candidate)) {
            throw new IllegalArgumentException("候选项不属于当前掉落来源");
        }
        this.selected = candidate;
    }

    /**
     * 来源选中的宝石, 未选择时为空.
     */
    public @Nullable DropCandidate getSelected() {
        return selected;
    }
}
