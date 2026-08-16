package me.qscbm.inlayx.api;

import lombok.NonNull;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 掉落候选项.
 */
public record DropCandidate(@NonNull Gem gem, @NonNull ConfigurationSection settings) {}
