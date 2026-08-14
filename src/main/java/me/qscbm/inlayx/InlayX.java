package me.qscbm.inlayx;

import lombok.Getter;
import me.qscbm.inlayx.command.GemCommand;
import me.qscbm.inlayx.config.ConfigManager;
import me.qscbm.inlayx.config.ConfigUpdater;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.listener.ExtractGuiListener;
import me.qscbm.inlayx.listener.GuiListener;
import me.qscbm.inlayx.listener.MobListener;
import me.qscbm.inlayx.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class InlayX extends JavaPlugin {
    public static InlayX INSTANCE;

    private GemManager gemManager;

    private ConfigManager configManager;

    private ExtractGuiListener extractGuiListener;

    @Override
    public void onEnable() {
        this.getLogger().info("""

                #############################################
                  ___           _                  __  __ \s
                 |_ _|  _ __   | |   __ _   _   _  \\ \\/ / \s
                  | |  | '_ \\  | |  / _` | | | | |  \\  /  \s
                  | |  | | | | | | | (_| | | |_| |  /  \\  \s
                 |___| |_| |_| |_|  \\__,_|  \\__, | /_/\\_\\ \s
                                            |___/         \s
                ##############################################
                """);
        this.getLogger().info("InlayX 正在启动中");
        this.saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml");

        this.getLogger().info("加载配置文件中......");
        reloadConfig();
        this.configManager = new ConfigManager(this);
        this.getLogger().info("配置文件已加载");

        this.getLogger().info("加载宝石中......");
        this.gemManager = new GemManager(this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        this.extractGuiListener = new ExtractGuiListener(this);
        this.getServer().getPluginManager().registerEvents(this.extractGuiListener, this);
        this.getServer().getPluginManager().registerEvents(new MobListener(this), this);
        if (this.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.getLogger().info("已检测到MythicMobs插件, 启用MythicMobs支持");
        }
        this.getCommand("gem").setExecutor(new GemCommand(this));

        INSTANCE = this;

        this.getLogger().info("InlayX 已启用");
    }

    public void onDisable() {
        if (this.extractGuiListener != null) {
            this.extractGuiListener.cancelTasks();
        }
        INSTANCE = null;
        this.getLogger().info("InlayX 已禁用");
    }
}
