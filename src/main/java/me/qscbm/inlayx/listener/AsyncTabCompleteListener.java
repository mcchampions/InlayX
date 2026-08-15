package me.qscbm.inlayx.listener;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import java.util.List;
import me.qscbm.inlayx.command.GemCommand;
import me.qscbm.inlayx.command.GemTabCompleter;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AsyncTabCompleteListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onAsyncTabCompleteEvent(AsyncTabCompleteEvent e) {
        if (!e.isCommand()) {
            return;
        }
        String buffer = e.getBuffer();
        if (buffer.isEmpty()) {
            return;
        }
        if (buffer.charAt(0) == '/') {
            buffer = buffer.substring(1);
        }

        int firstPlace = buffer.indexOf(' ');
        if (firstPlace < 0) {
            return;
        }
        String commandLabel = buffer.substring(0, firstPlace).toLowerCase();
        if (!GemCommand.COMMAND_ALIASES.contains(commandLabel) && !"slimefun".equals(commandLabel)) {
            int index = commandLabel.indexOf("slimefun:");
            if (index != 0) {
                return;
            }
            commandLabel = commandLabel.substring(9);
            if (!GemCommand.COMMAND_ALIASES.contains(commandLabel) && !"slimefun".equals(commandLabel)) {
                return;
            }
        }
        List<String> args = TextUtils.tokenize(buffer.substring(firstPlace + 1));
        List<String> suggests = GemTabCompleter.onTabComplete(e.getSender(), args);
        e.setCompletions(suggests);
        e.setHandled(true);
    }
}
