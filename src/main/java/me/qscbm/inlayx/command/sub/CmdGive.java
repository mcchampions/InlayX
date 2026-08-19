package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 给予宝石
 */
public class CmdGive extends SubCommand {
    public CmdGive(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String description() {
        return i18n("command.give.description");
    }

    @Override
    protected String usage() {
        return i18n("command.give.usage");
    }

    @Override
    public String permission() {
        return "inlayx.give";
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String gemId = args[1];
        int amount = argInt(args, 2, 1, 64);
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(i18n("command.give.player_not_found", playerName));
            return;
        }
        GemManager gm = plugin.getGemManager();
        Gem gem = gm.getGem(gemId);
        if (gem == null) {
            sender.sendMessage(i18n("command.give.gem_not_found", gemId));
            return;
        }
        ItemStack item = gm.createGemItem(gemId);
        if (item == null) {
            sender.sendMessage(i18n("command.give.create_failed", gemId));
            return;
        }
        item.setAmount(amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
        int rest = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        int given = amount - rest;
        if (given > 0) {
            sender.sendMessage(i18n("command.give.given_to_sender", given, gem.getName(), target.getName()));
            target.sendMessage(i18n("command.give.given_to_target", given, gem.getName()));
        }
        if (rest > 0) {
            if (plugin.getConfigManager().isDropGemOnFullInventory()) {
                for (ItemStack left : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), left);
                }
                target.sendMessage(i18n("command.give.full_drop_target", rest, gem.getName()));
                sender.sendMessage(i18n("command.give.full_drop_sender", target.getName(), rest, gem.getName()));
            } else {
                target.sendMessage(i18n("command.give.full_deny_target", rest, gem.getName()));
                sender.sendMessage(i18n("command.give.full_deny_sender", target.getName(), rest, gem.getName()));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return plugin.getGemManager().getAllGems().stream().map(Gem::getId).collect(Collectors.toList());
        }
        if (args.length == 3) {
            return IntStream.rangeClosed(1, 64).mapToObj(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }
}
