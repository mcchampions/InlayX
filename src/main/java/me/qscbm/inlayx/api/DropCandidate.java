package me.qscbm.inlayx.api;

import me.qscbm.inlayx.gem.Gem;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

/**
 * 掉落候选项.
 */
public record DropCandidate(@NonNull Gem gem, @NonNull ConfigurationSection settings) {}
