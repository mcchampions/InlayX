package me.qscbm.inlayx.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PlayerListenerTest extends InlayXTestBase {

    private PlayerMock player;
    private GemType attack;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        attack = plugin.getConfigManager().getGemType("ATTACK");
    }

    private PlayerInteractEvent rightClick(ItemStack gemItem) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, gemItem, null, BlockFace.UP, EquipmentSlot.HAND);
    }

    @Test
    void rightClickSocketsGemIntoOffhand() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack gemItem = plugin.getGemManager().createGemItem("t1");
        ItemStack sword = socketableSword(attack, 1);
        player.getInventory().setItemInMainHand(gemItem);
        player.getInventory().setItemInOffHand(sword);
        PlayerInteractEvent event = rightClick(player.getInventory().getItemInMainHand());
        server.getPluginManager().callEvent(event);
        ItemStack hand = player.getInventory().getItemInMainHand();
        assertTrue(hand == null || hand.getType() == Material.AIR);
        assertTrue(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInOffHand())
                .contains("t1"));
    }

    @Test
    void rightClickDisabledByConfig() {
        registerGem("t1", "ATTACK", 1.0);
        plugin.getConfig().set("settings.socket.quick_socket.right_click", false);
        plugin.getConfigManager().loadSettings();
        assertFalse(plugin.getConfigManager().isRightClickSocketEnabled());
        ItemStack gemItem = plugin.getGemManager().createGemItem("t1");
        ItemStack sword = socketableSword(attack, 1);
        player.getInventory().setItemInMainHand(gemItem);
        player.getInventory().setItemInOffHand(sword);
        PlayerInteractEvent event = rightClick(gemItem);
        server.getPluginManager().callEvent(event);
        assertTrue(plugin.getGemManager().isGem(player.getInventory().getItemInMainHand()));
        assertTrue(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInOffHand())
                .isEmpty());
    }

    @Test
    void rightClickFailureDestroysBrokenGem() {
        registerGem("t3", "ATTACK", 0.0);
        plugin.getGemManager().getGems().get("t3").setDestroyOnFailure(true);
        ItemStack gemItem = plugin.getGemManager().createGemItem("t3");
        ItemStack sword = socketableSword(attack, 1);
        player.getInventory().setItemInMainHand(gemItem);
        player.getInventory().setItemInOffHand(sword);
        PlayerInteractEvent event = rightClick(gemItem);
        server.getPluginManager().callEvent(event);
        ItemStack hand = player.getInventory().getItemInMainHand();
        assertTrue(hand == null || hand.getType() == Material.AIR);
        assertTrue(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInOffHand())
                .isEmpty());
    }
}
