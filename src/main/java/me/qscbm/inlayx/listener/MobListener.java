package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.event.GemDropEvent;
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
            dropSelectedGem(event, entity, killer, "normal", 1);
            return;
        }
        dropSelectedGem(event, entity, killer, "mythic", mythicMobs.getMobLevel(entity));
    }

    private void dropSelectedGem(
            EntityDeathEvent event, LivingEntity entity, Player killer, String source, int mobLevel) {
        Gem gem = gm().getDropGem(source, mobLevel);
        if (gem == null) {
            return;
        }
        GemDropEvent dropEvent = new GemDropEvent(entity, killer, source, mobLevel, gem);
        if (!dropEvent.callEvent() || dropEvent.getGem() == null) {
            return;
        }
        event.getDrops().add(gm().createGemItem(dropEvent.getGem().getId()));
    }

    private GemManager gm() {
        return plugin.getGemManager();
    }
}
