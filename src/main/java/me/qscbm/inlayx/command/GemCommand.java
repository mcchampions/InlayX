package me.qscbm.inlayx.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.sub.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

/**
 * 命令执行器/补全器
 * <p>
 * 调度命令的执行与补全
 */
public class GemCommand implements CommandExecutor {
    public static Set<String> COMMAND_ALIASES;
    public static Map<String, SubCommand> COMMANDS;

    public Map<String, SubCommand> getCommands() {
        return COMMANDS;
    }

    /**
     * 注册子命令.
     */
    public static boolean registerSubCommand(@NonNull SubCommand subCommand) {
        return COMMANDS.putIfAbsent(subCommand.name().toLowerCase(), subCommand) == null;
    }

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
        all.addFirst(new CmdHelp(plugin));
        COMMANDS = all.stream()
                .collect(Collectors.toMap(
                        SubCommand::name,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new));
        COMMAND_ALIASES = plugin.getServer().getPluginCommand("gem").getAliases().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length == 0) {
            COMMANDS.get("help").tryExecute(sender, args);
            return true;
        }
        String sub = args[0].toLowerCase();
        String[] rest = shift(args);

        SubCommand cmd = COMMANDS.get(sub);
        if (cmd == null) {
            sender.sendMessage(ChatColor.RED + "未知命令, 输入 /gem help 查看帮助");
            return true;
        }
        cmd.tryExecute(sender, rest);
        return true;
    }

    private static String[] shift(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }
}
