package me.qscbm.inlayx.drop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

final class DropMath {
    private DropMath() {}

    static <T> @Nullable T selectWeighted(List<T> values, ToDoubleFunction<T> weightFunction) {
        List<Entry<T>> entries = new ArrayList<>();
        double total = 0;
        for (T value : values) {
            double weight = Math.clamp(weightFunction.applyAsDouble(value), 0.0, 1.0);
            if (weight <= 0) {
                continue;
            }
            entries.add(new Entry<>(value, weight));
            total += weight;
        }
        if (entries.isEmpty() || total <= 0) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll >= Math.min(1.0, total)) {
            return null;
        }
        double target = roll * total;
        double cumulative = 0;
        for (Entry<T> entry : entries) {
            cumulative += entry.weight;
            if (target < cumulative) {
                return entry.value;
            }
        }
        return entries.getLast().value;
    }

    private record Entry<T>(T value, double weight) {}
}
