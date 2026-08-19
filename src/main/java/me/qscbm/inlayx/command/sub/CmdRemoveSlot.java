package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.GemTabCompleter;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 移除指定槽位
 */
public class CmdRemoveSlot extends SubCommand {
    public CmdRemoveSlot(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "removeslot";
    }

    @Override
    public String description() {
        return i18n("command.removeslot.description");
    }

    @Override
    protected String usage() {
        return i18n("command.removeslot.usage");
    }

    @Override
    public String permission() {
        return "inlayx.removeslot";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(i18n("command.removeslot.must_hold_equipment"));
            return;
        }
        GemType type = args.length > 0
                ? plugin.getConfigManager().getGemType(args[0])
                : plugin.getConfigManager().getDefaultGemType();
        if (type == null) {
            if (args.length > 0) {
                player.sendMessage(i18n("command.removeslot.invalid_type", args[0]));
            } else {
                player.sendMessage(i18n("command.removeslot.no_type_configured"));
            }
            return;
        }
        int count = argInt(args, 1, 1, plugin.getConfigManager().getMaxSockets());
        int before = gm.getSocketCount(item);
        player.getInventory().setItemInMainHand(gm.removeSlotFromItem(item, count, type));
        int removed = before - gm.getSocketCount(player.getInventory().getItemInMainHand());
        if (removed == 0) {
            player.sendMessage(i18n("command.removeslot.none_removable", type.name()));
            return;
        }
        if (removed < count) {
            player.sendMessage(i18n("command.removeslot.removed_partial", removed, type.name()));
            return;
        }
        player.sendMessage(i18n("command.removeslot.removed", removed, type.name()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return GemTabCompleter.filter(
                    plugin.getConfigManager().getGemTypes().keySet().stream().toList(), args[0]);
        }
        if (args.length == 2) {
            return IntStream.rangeClosed(1, plugin.getConfigManager().getMaxSockets())
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
