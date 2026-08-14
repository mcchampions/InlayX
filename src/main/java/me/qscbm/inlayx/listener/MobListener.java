package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.integration.MythicMobsBridge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobListener implements Listener {
    private final InlayX plugin;
    private final MythicMobsBridge mythicMobs;

    public MobListener(InlayX plugin) {
        this.plugin = plugin;
        this.mythicMobs = new MythicMobsBridge(plugin);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfigManager().isDropSystemEnabled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        if (!mythicMobs.isMythicMob(entity)) {
            if (!(entity instanceof Mob)) {
                return;
            }
            Gem gem = gm().getDropGem("normal", 1);
            if (gem != null) {
                event.getDrops().add(gm().createGemItem(gem.getId()));
            }
            return;
        }
        Gem gem = gm().getDropGem("mythic", mythicMobs.getMobLevel(entity));
        if (gem != null) {
            event.getDrops().add(gm().createGemItem(gem.getId()));
        }
    }

    private GemManager gm() {
        return plugin.getGemManager();
    }
}
