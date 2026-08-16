package me.qscbm.inlayx.gem;

import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.ChatColor;

/**
 * 宝石显示模板的变量解析
 * <p>
 * 可用变量: {gemTypeColor} 类型颜色, {gemName} 宝石名称, {gemTypeName} 类型名称,
 * {gemLevelStars} 等级星星, {gemLevel} 等级数字, {successRate} 镶嵌成功率,
 * {destroyOnFailure} 失败是否破坏, {gemId} 宝石 id.
 * 额外变量经 extraVariables 成对追加.
 */
public final class GemTemplate {
    private GemTemplate() {}

    public static String parse(String text, Gem gem, String... extraVariables) {
        GemType type = gem.getType();
        ChatColor color = type.color();
        String parsed = TextUtils.translateAlternateColorCodes(text);
        parsed = parsed.replace("{gemTypeColor}", color.toString())
                .replace("{gemName}", gem.getName())
                .replace("{gemTypeName}", type.name())
                .replace("{gemLevelStars}", TextUtils.getStars(gem.getLevel()))
                .replace("{gemLevel}", String.valueOf(gem.getLevel()))
                .replace("{successRate}", gem.getSocketSuccessRate() * 100 + "%")
                .replace("{destroyOnFailure}", gem.isDestroyOnFailure() ? "是" : "否")
                .replace("{gemId}", gem.getId());
        for (int i = 0; i + 1 < extraVariables.length; i += 2) {
            parsed = parsed.replace(extraVariables[i], extraVariables[i + 1]);
        }
        return parsed;
    }
}
