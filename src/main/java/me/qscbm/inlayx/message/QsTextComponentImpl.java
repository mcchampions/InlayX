package me.qscbm.inlayx.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.internal.Internals;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class QsTextComponentImpl implements TextComponent {
    protected List<Component> children;
    protected Style style;

    @Override
    public final @NonNull List<Component> children() {
        return this.children;
    }

    @Override
    public final @NonNull Style style() {
        return this.style;
    }

    private String content;

    public QsTextComponentImpl(String content) {
        this.children = Collections.emptyList();
        this.style = Style.empty();
        this.content = content;
    }

    public QsTextComponentImpl(List<Component> components, Style style, String content) {
        this.children = components;
        this.style = style;
        this.content = content;
    }

    public QsTextComponentImpl(List<Component> components, String content) {
        this.children = components;
        this.style = Style.empty();
        this.content = content;
    }

    public QsTextComponentImpl(Style style, String content) {
        this.children = Collections.emptyList();
        this.style = style;
        this.content = content;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public QsTextComponentImpl clone() {
        return new QsTextComponentImpl(
                new ArrayList<>(this.children), this.style.toBuilder().build(), this.content);
    }

    @Override
    public @NonNull String content() {
        return this.content;
    }

    @Override
    public @NonNull QsTextComponentImpl append(final ComponentLike like) {
        final Component component = like.asComponent();
        if (component == Component.empty()) return this;
        if (children.equals(Collections.emptyList())) {
            children = new ArrayList<>();
        }
        children.add(component);
        return this;
    }

    public QsTextComponentImpl append(final ComponentLike like, final TextColor defaultColor) {
        Component component = like.asComponent();
        if (component == Component.empty()) return this;
        if (component.color() == null) {
            component = component.color(defaultColor);
        }
        if (children.equals(Collections.emptyList())) {
            children = new ArrayList<>();
        }
        children.add(component);
        return this;
    }

    @Override
    public @NonNull QsTextComponentImpl append(final @NonNull Component component) {
        if (component == Component.empty()) return this;
        if (children.equals(Collections.emptyList())) {
            children = new ArrayList<>();
        }
        children.add(component);
        return this;
    }

    public QsTextComponentImpl append(final QsTextComponentImpl like) {
        if (children.equals(Collections.emptyList())) {
            children = new ArrayList<>();
        }
        children.add(like);
        return this;
    }

    @Override
    public @NonNull QsTextComponentImpl color(TextColor color) {
        return style(style.color(color));
    }

    @Override
    public @NonNull QsTextComponentImpl content(final @NonNull String content) {
        this.content = content;
        return this;
    }

    @Override
    public @NonNull QsTextComponentImpl children(final List<? extends ComponentLike> children) {
        List<Component> components = new ArrayList<>();
        for (ComponentLike like : children) {
            Component component = like.asComponent();
            components.add(component);
        }
        this.children = components;
        return this;
    }

    @Override
    public @NonNull QsTextComponentImpl style(final @NonNull Style style) {
        this.style = style;
        return this;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof TextComponent that)) return false;
        return Objects.equals(this.children, that.children())
                && Objects.equals(this.style, that.style())
                && Objects.equals(this.content, that.content());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = (31 * result) + this.content.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Internals.toString(this);
    }

    @Override
    public @NonNull Builder toBuilder() {
        return Component.text();
    }
}
