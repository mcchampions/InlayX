package me.qscbm.inlayx.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.sub.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

/**
 * 命令执行器/补全器
 * <p>
 * 调度命令的执行与补全
 */
public class GemCommand implements CommandExecutor, TabCompleter {
    @Getter
    private final Map<String, SubCommand> commands;

    public GemCommand(InlayX plugin) {
        List<SubCommand> others = List.of(
                new CmdList(plugin),
                new CmdGive(plugin),
                new CmdAddGem(plugin),
                new CmdSocket(plugin),
                new CmdExtract(plugin),
                new CmdRemoveGem(plugin),
                new CmdInfo(plugin),
                new CmdAddSlot(plugin),
                new CmdRemoveSlot(plugin),
                new CmdReload(plugin));
        List<SubCommand> all = new ArrayList<>(others);
        all.addFirst(new CmdHelp(plugin, others));
        commands = all.stream().collect(Collectors.toMap(SubCommand::name, Function.identity()));
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length == 0) {
            commands.get("help").tryExecute(sender, args);
            return true;
        }
        String sub = args[0].toLowerCase();
        String[] rest = shift(args);

        SubCommand cmd = commands.get(sub);
        if (cmd == null) {
            sender.sendMessage(ChatColor.RED + "未知命令, 输入 /gem help 查看帮助");
            return true;
        }
        cmd.tryExecute(sender, rest);
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> visible = new ArrayList<>();
            for (SubCommand cmd : commands.values()) {
                if (cmd.permission() == null || SubCommand.hasAny(sender, cmd.permission())) {
                    visible.add(cmd.name());
                }
            }
            return filter(visible, prefix);
        }
        SubCommand cmd = commands.get(args[0].toLowerCase());
        if (cmd == null) return List.of();
        return cmd.tabComplete(sender, shift(args));
    }

    private static String[] shift(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }

    private static List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
    }
}
