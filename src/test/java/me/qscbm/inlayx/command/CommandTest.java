package me.qscbm.inlayx.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.command.sub.CmdAddGem;
import me.qscbm.inlayx.command.sub.CmdAddSlot;
import me.qscbm.inlayx.command.sub.CmdExtract;
import me.qscbm.inlayx.command.sub.CmdGive;
import me.qscbm.inlayx.command.sub.CmdHelp;
import me.qscbm.inlayx.command.sub.CmdList;
import me.qscbm.inlayx.command.sub.CmdRemoveGem;
import me.qscbm.inlayx.command.sub.CmdRemoveSlot;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class CommandTest extends InlayXTestBase {

    private PlayerMock player;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        player.addAttachment(plugin, "inlayx.admin", true);
    }

    @Test
    void helpListsCommands() {
        ConsoleCommandSenderMock sender = server.getConsoleSender();
        new CmdHelp(plugin, List.of(new CmdList(plugin), new CmdGive(plugin))).tryExecute(sender, new String[0]);
        assertEquals(ChatColor.GOLD + "===== InlayX 帮助 =====", sender.nextMessage());
    }

    @Test
    void giveAddsGemToPlayerInventory() {
        registerGem("t1", "ATTACK", 1.0);
        new CmdGive(plugin).tryExecute(server.getConsoleSender(), new String[] {"Steve", "t1"});
        assertTrue(Arrays.stream(player.getInventory().getContents())
                .anyMatch(i -> i != null && plugin.getGemManager().isGem(i)));
    }

    @Test
    void addSlotAndRemoveSlot() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        player.getInventory().setItemInMainHand(sword);
        new CmdAddSlot(plugin).tryExecute(player, new String[] {"ATTACK", "2"});
        assertEquals(
                2, plugin.getGemManager().getSocketCount(player.getInventory().getItemInMainHand()));
        new CmdRemoveSlot(plugin).tryExecute(player, new String[] {"ATTACK", "2"});
        assertEquals(
                0, plugin.getGemManager().getSocketCount(player.getInventory().getItemInMainHand()));
    }

    @Test
    void addGemAndExtractFlow() {
        registerGem("t1", "ATTACK", 1.0);
        setExtractRate(1.0);
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 1);
        player.getInventory().setItemInMainHand(sword);
        new CmdAddGem(plugin).tryExecute(player, new String[] {"t1"});
        assertTrue(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInMainHand())
                .contains("t1"));
        new CmdExtract(plugin).tryExecute(player, new String[] {"t1"});
        assertTrue(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInMainHand())
                .isEmpty());
        assertTrue(Arrays.stream(player.getInventory().getContents())
                .anyMatch(i -> i != null && plugin.getGemManager().isGem(i)));
    }

    @Test
    void extractUnknownGemRemovesWithoutRoll() {
        registerGem("t1", "ATTACK", 1.0);
        setExtractRate(0.0);
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        plugin.getGemManager().unregisterGem("t1");
        player.getInventory().setItemInMainHand(sword);

        new CmdExtract(plugin).tryExecute(player, new String[] {"t1"});

        assertFalse(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInMainHand())
                .contains("t1"));
    }

    @Test
    void removeUnknownGem() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        plugin.getGemManager().unregisterGem("t1");
        player.getInventory().setItemInMainHand(sword);

        new CmdRemoveGem(plugin).tryExecute(player, new String[] {"t1"});

        assertFalse(plugin.getGemManager()
                .getSocketedGems(player.getInventory().getItemInMainHand())
                .contains("t1"));
    }
}
