package me.qscbm.inlayx.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import me.qscbm.inlayx.command.sub.SubCommand;
import org.bukkit.command.CommandSender;

public class GemTabCompleter {

    public static List<String> onTabComplete(CommandSender sender, List<String> args) {
        if (args.size() == 1) {
            String prefix = args.getFirst().toLowerCase();
            List<String> visible = new ArrayList<>();
            for (SubCommand cmd : GemCommand.COMMANDS.values()) {
                if (cmd.permission() == null || SubCommand.hasAny(sender, cmd.permission())) {
                    visible.add(cmd.name());
                }
            }
            return filter(visible, prefix);
        }
        SubCommand cmd = GemCommand.COMMANDS.get(args.getFirst().toLowerCase());
        if (cmd == null) return List.of();
        return cmd.tabComplete(sender, shift(args.toArray(new String[0])));
    }

    private static List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
    }

    private static String[] shift(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }
}
