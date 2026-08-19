package me.qscbm.inlayx;

import com.tcoded.folialib.FoliaLib;
import lombok.Getter;
import lombok.NonNull;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.InlayXApi;
import me.qscbm.inlayx.command.GemCommand;
import me.qscbm.inlayx.command.sub.SubCommand;
import me.qscbm.inlayx.config.ConfigManager;
import me.qscbm.inlayx.config.ConfigUpdater;
import me.qscbm.inlayx.config.DropSourceConfigManager;
import me.qscbm.inlayx.config.ItemGroupConfigManager;
import me.qscbm.inlayx.drop.DropCoordinator;
import me.qscbm.inlayx.drop.DropSourceRegistry;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.interaction.InteractionFeedback;
import me.qscbm.inlayx.listener.AsyncTabCompleteListener;
import me.qscbm.inlayx.listener.ExtractGuiListener;
import me.qscbm.inlayx.listener.GuiListener;
import me.qscbm.inlayx.listener.MobListener;
import me.qscbm.inlayx.listener.PlayerListener;
import me.qscbm.inlayx.service.LanguageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件主类
 */
@Getter
public class InlayX extends JavaPlugin implements InlayXApi {
    public static InlayX INSTANCE;

    private GemManager gemManager;

    private DropSourceRegistry dropSourceRegistry;

    private DropSourceConfigManager dropSourceConfigManager;

    private DropCoordinator dropCoordinator;

    private ConfigManager configManager;

    private FoliaLib foliaLib;

    private ExtractGuiListener extractGuiListener;

    private InteractionFeedback interactionFeedback;

    private LanguageService languageService;

    private ItemGroupConfigManager itemGroupConfigManager;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.getLogger().info("""

            #############################################
              ___           _                  __  __ \s
             |_ _|  _ __   | |   __ _   _   _  \\ \\/ / \s
               | |  | '_ \\  | |  / _` | | | | |  \\  /  \s
               | |  | | | | | | | (_| | | |_| |  /  \\  \s
              |___| |_| |_| |_|  \\__,_|  \\__, | /_/\\_\\ \s
                                        |___/         \s
            #############################################
            """);
        this.getLogger().info("InlayX 正在启动中");
        this.saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml");

        this.getLogger().info("加载配置文件中......");
        reloadConfig();
        this.configManager = new ConfigManager(this);
        this.foliaLib = new FoliaLib(this);

        this.languageService = new LanguageService(this);
        this.getLogger().info("加载物品组中......");
        this.itemGroupConfigManager = new ItemGroupConfigManager(this);
        itemGroupConfigManager.load();
        this.getLogger().info("配置文件已加载");

        this.getLogger().info("加载掉落来源中......");
        this.dropSourceRegistry = DropSourceRegistry.createDefault(this);
        this.dropSourceConfigManager = new DropSourceConfigManager(this, dropSourceRegistry);
        this.dropSourceConfigManager.load();

        this.getLogger().info("加载宝石中......");
        this.gemManager = new GemManager(this);
        this.interactionFeedback = new InteractionFeedback(this);
        this.dropCoordinator = new DropCoordinator(this);
        this.getCommand("gem").setExecutor(new GemCommand(this));
        this.getServer().getServicesManager().register(InlayXApi.class, this, this, ServicePriority.Normal);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AsyncTabCompleteListener(), this);
        this.extractGuiListener = new ExtractGuiListener(this);
        this.getServer().getPluginManager().registerEvents(this.extractGuiListener, this);
        this.getServer().getPluginManager().registerEvents(new MobListener(this), this);
        if (this.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.getLogger().info("已检测到MythicMobs插件, 启用MythicMobs支持");
        }

        this.getLogger().info("InlayX 已启用");
    }

    @Override
    public boolean registerSubCommand(@NonNull SubCommand subCommand) {
        return GemCommand.registerSubCommand(subCommand);
    }

    @Override
    public boolean registerDropSource(@NonNull DropSource dropSource) {
        if (!dropSourceRegistry.register(dropSource)) {
            return false;
        }
        dropSourceConfigManager.onSourceRegistered(dropSource);
        return true;
    }

    /**
     * 获取 API 实例
     */
    public static InlayXApi getApi() {
        return Bukkit.getServicesManager().load(InlayXApi.class);
    }

    public void onDisable() {
        if (this.extractGuiListener != null) {
            this.extractGuiListener.cancelTasks();
        }
        this.getServer().getServicesManager().unregister(this);
        INSTANCE = null;
        this.getLogger().info("InlayX 已禁用");
    }
}
