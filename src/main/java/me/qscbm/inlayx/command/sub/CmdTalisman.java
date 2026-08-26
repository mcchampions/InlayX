package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.talisman.Talisman;
import me.qscbm.inlayx.talisman.TalismanEffect;
import me.qscbm.inlayx.talisman.TalismanManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 保护符管理
 */
public class CmdTalisman extends SubCommand {
    public CmdTalisman(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "talisman";
    }

    @Override
    public String description() {
        return i18n("command.talisman.description");
    }

    @Override
    protected String usage() {
        return i18n("command.talisman.usage");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(i18n("common.player_only"));
                return;
            }
            if (noneOf(sender, "inlayx.talisman.use")) {
                sender.sendMessage(i18n("common.no_permission"));
                return;
            }
            player.openInventory(plugin.getTalismanManager().createApplyGUI());
            return;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> executeGive(sender, shift(args));
            case "list" -> executeList(sender);
            case "info" -> executeInfo(sender);
            default -> sender.sendMessage(i18n("command.talisman.unknown_sub", args[0]));
        }
    }

    private void executeGive(CommandSender sender, String[] args) {
        if (noneOf(sender, "inlayx.talisman.give")) {
            sender.sendMessage(i18n("common.no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(i18n("common.usage", i18n("command.talisman.give_usage")));
            return;
        }
        String playerName = args[0];
        String talismanId = args[1];
        if (playerName == null) {
            sender.sendMessage(i18n("common.usage", i18n("command.talisman.give_usage")));
            return;
        }
        int amount = argInt(args, 2, 1, 64);

        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(i18n("command.talisman.player_not_found", playerName));
            return;
        }
        TalismanManager tm = plugin.getTalismanManager();
        Talisman talisman = tm.getTalisman(talismanId);
        if (talisman == null) {
            sender.sendMessage(i18n("command.talisman.talisman_not_found", talismanId));
            return;
        }
        ItemStack item = tm.createTalismanItem(talismanId);
        if (item == null) {
            sender.sendMessage(i18n("command.talisman.create_failed", talismanId));
            return;
        }
        item.setAmount(amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
        int rest = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        int given = amount - rest;
        if (given > 0) {
            sender.sendMessage(i18n("command.talisman.given_to_sender", given, talisman.getName(), target.getName()));
            target.sendMessage(i18n("command.talisman.given_to_target", given, talisman.getName()));
        }
        if (rest > 0) {
            if (plugin.getConfigManager().isDropGemOnFullInventory()) {
                for (ItemStack left : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), left);
                }
                target.sendMessage(i18n("command.talisman.full_drop_target", rest, talisman.getName()));
                sender.sendMessage(
                        i18n("command.talisman.full_drop_sender", target.getName(), rest, talisman.getName()));
            } else {
                target.sendMessage(i18n("command.talisman.full_deny_target", rest, talisman.getName()));
                sender.sendMessage(
                        i18n("command.talisman.full_deny_sender", target.getName(), rest, talisman.getName()));
            }
        }
    }

    private void executeList(CommandSender sender) {
        if (noneOf(sender, "inlayx.talisman.list")) {
            sender.sendMessage(i18n("common.no_permission"));
            return;
        }
        List<Talisman> all = plugin.getTalismanManager().getAllTalismans();
        if (all.isEmpty()) {
            sender.sendMessage(i18n("command.talisman.list_empty"));
            return;
        }
        sender.sendMessage(i18n("command.talisman.list_title"));
        for (Talisman talisman : all) {
            int percent = (int) Math.round(talisman.getFunction().successRateBonus() * 100);
            String bonus = percent > 0 ? "+" + percent + "%" : i18n("command.talisman.none");
            String prevent = talisman.getFunction().preventDestroy()
                    ? i18n("command.talisman.yes")
                    : i18n("command.talisman.no");
            sender.sendMessage(i18n(
                    "command.talisman.list_entry",
                    talisman.getId(),
                    talisman.getName(),
                    talisman.getMaxUses(),
                    bonus,
                    prevent));
        }
    }

    private void executeInfo(CommandSender sender) {
        if (noneOf(sender, "inlayx.talisman.info")) {
            sender.sendMessage(i18n("common.no_permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n("common.player_only"));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || !plugin.getGemManager().isGem(held)) {
            player.sendMessage(i18n("command.talisman.info_must_hold_gem"));
            return;
        }
        TalismanEffect.State state = plugin.getTalismanManager().readEffects(held);
        if (state == null || state.isEmpty()) {
            player.sendMessage(i18n("command.talisman.info_none"));
            return;
        }
        player.sendMessage(i18n("command.talisman.info_title"));
        if (state.bonus() != null) {
            int percent = (int) Math.round(state.bonus().bonus() * 100);
            player.sendMessage(
                    i18n("command.talisman.info_bonus", percent, state.bonus().uses()));
        }
        if (state.prevent() != null) {
            player.sendMessage(
                    i18n("command.talisman.info_prevent", state.prevent().uses()));
        }
    }

    private static String[] shift(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("give", "list", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return plugin.getTalismanManager().getAllTalismans().stream()
                    .map(Talisman::getId)
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && "give".equalsIgnoreCase(args[0])) {
            return IntStream.rangeClosed(1, 64).mapToObj(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }
}
