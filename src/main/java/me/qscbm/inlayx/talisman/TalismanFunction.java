package me.qscbm.inlayx.talisman;

/**
 * 保护符的功能配置.
 *
 * @param successRateBonus 镶嵌成功率加成 (0~1)
 * @param preventDestroy   镶嵌失败时保护宝石不碎裂
 */
public record TalismanFunction(double successRateBonus, boolean preventDestroy) {

    public static TalismanFunction empty() {
        return new TalismanFunction(0.0, false);
    }

    /**
     * 是否配置了至少一种功能.
     */
    public boolean hasAnyEffect() {
        return successRateBonus > 0 || preventDestroy;
    }
}
