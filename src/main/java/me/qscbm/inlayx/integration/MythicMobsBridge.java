package me.qscbm.inlayx.integration;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.util.ReflectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * MythicMobs 桥接
 * <p>
 * 理论支持 MythicMobs 4.x/5.x
 */
public class MythicMobsBridge {
    private static final String MM5_CLASS = "io.lumine.mythic.bukkit.MythicBukkit";
    private static final String MM4_CLASS = "io.lumine.xikage.mythic.bukkit.MythicBukkit";

    private final boolean mythicMobsPresent;
    private final Object mobManager;

    private final Method isMythicMobByEntity;
    private final Method isMythicMobByUuid;
    private final Method isActiveMob;

    private final Method getActiveMobByUuid;
    private final Method getActiveMobByEntity;

    private Method activeMobGetLevel;

    public MythicMobsBridge(InlayX plugin) {
        this.mythicMobsPresent = plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null;
        this.mobManager = init();
        this.isMythicMobByEntity = ReflectionUtils.findMethod(mobManager, "isMythicMob", Entity.class);
        this.isMythicMobByUuid = ReflectionUtils.findMethod(mobManager, "isMythicMob", UUID.class);
        this.isActiveMob = ReflectionUtils.findMethod(mobManager, "isActiveMob", Entity.class);
        this.getActiveMobByUuid = ReflectionUtils.findMethod(mobManager, "getActiveMob", UUID.class);
        this.getActiveMobByEntity = ReflectionUtils.findMethod(mobManager, "getActiveMob", Entity.class);
        logInitFailure(plugin);
    }

    private void logInitFailure(InlayX plugin) {
        if (!mythicMobsPresent) {
            return;
        }
        if (mobManager == null) {
            plugin.getLogger().severe("已检测到 MythicMobs 插件, 但无法通过反射初始化其 API, MythicMobs 掉落支持不可用!");
            return;
        }
        if (!canIdentify()) {
            plugin.getLogger()
                    .warning("已检测到 MythicMobs 插件, 但其 MobManager 缺少 isMythicMob/isActiveMob 方法, "
                            + "版本可能不兼容, MythicMobs 掉落支持不可用!");
        }
        if (getActiveMobByUuid == null && getActiveMobByEntity == null) {
            plugin.getLogger().warning("已检测到 MythicMobs 插件, 但其 MobManager 缺少 getActiveMob 方法, 怪物等级将按 1 处理!");
        }
    }

    private boolean canIdentify() {
        return isMythicMobByEntity != null || isMythicMobByUuid != null || isActiveMob != null;
    }

    public boolean isAvailable() {
        return mobManager != null && canIdentify();
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (!isAvailable()) {
            return false;
        }
        try {
            if (isMythicMobByEntity != null) {
                return (Boolean) isMythicMobByEntity.invoke(mobManager, entity);
            }
            if (isMythicMobByUuid != null) {
                return (Boolean) isMythicMobByUuid.invoke(mobManager, entity.getUniqueId());
            }
            return (Boolean) isActiveMob.invoke(mobManager, entity);
        } catch (Exception e) {
            return false;
        }
    }

    public int getMobLevel(LivingEntity entity) {
        if (mobManager == null) {
            return 1;
        }
        try {
            Object activeMob = null;
            if (getActiveMobByUuid != null) {
                Object result = getActiveMobByUuid.invoke(mobManager, entity.getUniqueId());
                if (result instanceof Optional<?> opt) {
                    activeMob = opt.orElse(null);
                } else {
                    activeMob = result;
                }
            } else if (getActiveMobByEntity != null) {
                activeMob = getActiveMobByEntity.invoke(mobManager, entity);
            }
            if (activeMob == null) {
                return 1;
            }
            if (activeMobGetLevel == null) {
                activeMobGetLevel = ReflectionUtils.findMethod(activeMob, "getLevel");
            }
            if (activeMobGetLevel == null) {
                return 1;
            }
            Object level = activeMobGetLevel.invoke(activeMob);
            return level instanceof Integer i ? i : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static Object init() {
        Object api = instantiate(MM5_CLASS);
        if (api == null) {
            api = instantiate(MM4_CLASS);
        }
        if (api == null) {
            return null;
        }
        try {
            return api.getClass().getMethod("getMobManager").invoke(api);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object instantiate(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getMethod("inst").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }
}
