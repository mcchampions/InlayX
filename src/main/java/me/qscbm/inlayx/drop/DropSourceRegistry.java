package me.qscbm.inlayx.drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.integration.MythicMobsBridge;
import org.jspecify.annotations.Nullable;

/**
 * 掉落来源注册表.
 * <p>
 * 注册表只负责保存来源, 注册顺序和默认设置, 不负责匹配与掉落判定.
 */
public final class DropSourceRegistry {
    private final AtomicReference<State> state;

    private DropSourceRegistry() {
        this.state = new AtomicReference<>(State.empty());
    }

    public static @NonNull DropSourceRegistry createDefault(@NonNull InlayX plugin) {
        DropSourceRegistry registry = new DropSourceRegistry();
        registry.registerBuiltIn(new MythicDropSource(new MythicMobsBridge(plugin)));
        registry.registerBuiltIn(new NormalDropSource());
        return registry;
    }

    /**
     * 注册一个掉落来源.
     *
     * @param source 要注册的来源
     * @return 注册成功返回 true, ID 重复时返回 false
     */
    public boolean register(@NonNull DropSource source) {
        RegisteredSource entry = createEntry(source);
        while (true) {
            State current = state.get();
            if (current.byId.containsKey(entry.source.id())) {
                return false;
            }
            State updated = current.append(entry);
            if (state.compareAndSet(current, updated)) {
                return true;
            }
        }
    }

    public @Nullable DropSource get(@NonNull String id) {
        RegisteredSource entry = state.get().byId.get(id);
        return entry == null ? null : entry.source;
    }

    public @NonNull List<DropSource> getSources() {
        return state.get().sources;
    }

    public @NonNull List<RegisteredSource> getEntries() {
        return state.get().entries;
    }

    public @NonNull Map<String, Object> getDefaultSettings(@NonNull String id) {
        RegisteredSource entry = state.get().byId.get(id);
        return entry == null ? Map.of() : entry.defaultSettings;
    }

    private void registerBuiltIn(DropSource source) {
        RegisteredSource entry = createEntry(source);
        while (true) {
            State current = state.get();
            if (current.byId.containsKey(entry.source.id())) {
                throw new IllegalArgumentException("内置掉落来源 ID 重复: " + entry.source.id());
            }
            State updated = current.append(entry);
            if (state.compareAndSet(current, updated)) {
                return;
            }
        }
    }

    private static RegisteredSource createEntry(DropSource source) {
        if (source == null) {
            throw new IllegalArgumentException("dropSource 不能为 null");
        }
        if (source.id().isBlank()) {
            throw new IllegalArgumentException("DropSource.id() 不能为空");
        }
        Map<String, Object> raw = source.defaultSettings();
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("enable", true);
        defaults.put("priority", 0);
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            defaults.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return new RegisteredSource(source, Collections.unmodifiableMap(defaults));
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(entry.getKey(), copyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(copyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Set<?> set) {
            return Set.copyOf(set);
        }
        return value;
    }

    /**
     * 已注册来源及其默认设置快照.
     */
    public record RegisteredSource(
            @NonNull DropSource source, @NonNull Map<String, Object> defaultSettings) {}

    private record State(List<RegisteredSource> entries, Map<String, RegisteredSource> byId, List<DropSource> sources) {
        static State empty() {
            return new State(List.of(), Map.of(), List.of());
        }

        State append(RegisteredSource entry) {
            List<RegisteredSource> nextEntries = new ArrayList<>(entries);
            Map<String, RegisteredSource> nextById = new LinkedHashMap<>(byId);
            List<DropSource> nextSources = new ArrayList<>(sources);
            nextEntries.add(entry);
            nextById.put(entry.source.id(), entry);
            nextSources.add(entry.source);
            return new State(
                    Collections.unmodifiableList(nextEntries),
                    Collections.unmodifiableMap(nextById),
                    Collections.unmodifiableList(nextSources));
        }
    }
}
