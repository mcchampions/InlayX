package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.drop.DropCoordinator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobListener implements Listener {
    private final DropCoordinator dropCoordinator;

    public MobListener(InlayX plugin) {
        this.dropCoordinator = plugin.getDropCoordinator();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        dropCoordinator.onEntityDeath(event);
    }
}
