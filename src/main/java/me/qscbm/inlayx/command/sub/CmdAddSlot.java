package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 添加指定槽位
 */
public class CmdAddSlot extends SubCommand {
    public CmdAddSlot(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "addslot";
    }

    @Override
    public String description() {
        return "为手持装备添加空槽位";
    }

    @Override
    public String permission() {
        return "inlayx.addslot";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected String usage() {
        return "/gem addslot <类型> [数量]";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "你必须手持一件装备");
            return;
        }
        GemType type = args.length > 0
                ? plugin.getConfigManager().getGemType(args[0])
                : plugin.getConfigManager().getDefaultGemType();
        if (type == null) {
            player.sendMessage(ChatColor.RED
                    + (args.length > 0 ? "无效的宝石类型: " + args[0] : "未配置任何宝石类型, 请先在 config.yml 的 settings.gem_types 中配置"));
            return;
        }
        int requested = argInt(args, 1, 1, plugin.getConfigManager().getMaxSockets());
        int maxSockets = plugin.getConfigManager().getMaxSockets();
        int current = gm.getSocketCount(item);
        int added = Math.max(0, Math.min(requested, maxSockets - current));
        if (added == 0) {
            player.sendMessage(ChatColor.RED + "该装备的宝石槽位已达上限(" + maxSockets + " 个)!");
            return;
        }
        player.getInventory().setItemInMainHand(gm.addSlotToItem(item, added, type));
        String message = ChatColor.GREEN + "已为装备添加 " + added + " 个 " + type.getName() + " 宝石槽位";
        if (added < requested) {
            message += ChatColor.YELLOW + "(已达上限 " + maxSockets + " 个)";
        }
        player.sendMessage(message);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getGemTypes().keySet().stream().toList();
        }
        if (args.length == 2) {
            return IntStream.rangeClosed(1, plugin.getConfigManager().getMaxSockets())
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
