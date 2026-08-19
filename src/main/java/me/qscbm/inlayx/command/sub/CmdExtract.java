package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.ExtractResult;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 提取宝石
 */
public class CmdExtract extends SubCommand {
    public CmdExtract(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "extract";
    }

    @Override
    public String description() {
        return i18n("command.extract.description");
    }

    @Override
    protected String usage() {
        return i18n("command.extract.usage");
    }

    @Override
    public String permission() {
        return "inlayx.extract";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (args.length == 0) {
            player.openInventory(plugin.getGemManager().getExtractGuiFactory().createGUI());
            return;
        }
        String gemId = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();

        if (item == null || item.getType() == Material.AIR || !gm.hasSocketedGems(item)) {
            player.sendMessage(i18n("command.extract.not_socketed"));
            return;
        }
        if (gm.getGem(gemId) == null) {
            if (!gm.getSocketedGems(item).contains(gemId)) {
                player.sendMessage(i18n("feedback.extract.not_found", gemId));
                return;
            }
            if (!gm.removeGem(item, gemId)) {
                player.sendMessage(i18n("feedback.extract.remove_unknown_failed"));
                return;
            }
            player.getInventory().setItemInMainHand(item);
            player.sendMessage(i18n("feedback.extract.removed_unknown", gemId));
            plugin.getInteractionFeedback().playExtractSound(player, true);
            return;
        }

        ExtractResult result = gm.extractGem(player, item, gemId);
        switch (result.getStatus()) {
            case SUCCESS -> {
                player.getInventory().setItemInMainHand(item);
                giveOrDrop(player, gm.createGemItem(result.getGemId()));
                player.sendMessage(i18n("feedback.extract.success"));
                plugin.getInteractionFeedback().playExtractSound(player, true);
            }
            case FAILED -> {
                player.getInventory().setItemInMainHand(item);
                player.sendMessage(i18n("feedback.extract.failed"));
                plugin.getInteractionFeedback().playExtractSound(player, false);
            }
            case CANCELLED -> player.sendMessage(i18n("feedback.extract.cancelled"));
            case NOT_FOUND -> player.sendMessage(i18n("feedback.extract.not_found", gemId));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player && plugin.getFoliaLib() != null) {
            CompletableFuture<List<String>> future = new CompletableFuture<>();
            try {
                plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    future.complete(plugin.getGemManager().getSocketedGems(item));
                });
                return future.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return plugin.getGemManager().getAllGems().stream()
                        .map(Gem::getId)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return plugin.getGemManager().getAllGems().stream()
                        .map(Gem::getId)
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null) return;
        if (player.getInventory().firstEmpty() != -1) player.getInventory().addItem(item);
        else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(i18n("command.extract.inventory_full_drop"));
        }
    }
}
