package me.qscbm.inlayx.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.message.QsTextComponentImpl;
import me.qscbm.inlayx.util.MCAssetsUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import org.bukkit.Material;
import org.json.JSONObject;

public class TranslationService {
    private final InlayX plugin;

    public TranslationService(InlayX plugin) {
        this.plugin = plugin;
        String version = plugin.getServer().getMinecraftVersion();
        JSONObject data = MCAssetsUtils.getLanguage(
                version,
                "zh_cn",
                plugin.getDataPath().resolve("cache").resolve("assets").toString());
        TranslationStore<Component> store = TranslationStore.component(Key.key("inlayx", "vanilla_translation"));
        store.registerAll(Locale.of("zh_cn"), getTranslationMap(data));
        GlobalTranslator.translator().addSource(store);
    }

    public Map<String, Component> getTranslationMap(JSONObject jsonObject) {
        Set<String> keys = jsonObject.keySet();
        Map<String, Component> translationMap = new HashMap<>(jsonObject.length());
        for (String key : keys) {
            String value = jsonObject.getString(key);
            translationMap.put(key, new QsTextComponentImpl(value));
        }
        return translationMap;
    }

    public String getMaterialI18nName(Material material) {
        TranslatableComponent component = Component.translatable(material);
        Component com = GlobalTranslator.translator().translate(component, Locale.of("zh_cn"));
        if (com instanceof TextComponent c) {
            return c.content();
        }
        return PlainTextComponentSerializer.plainText().serialize(com);
    }
}
