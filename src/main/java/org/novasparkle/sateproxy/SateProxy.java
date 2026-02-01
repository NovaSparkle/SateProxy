package org.novasparkle.sateproxy;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import lombok.Getter;
import lombok.SneakyThrows;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import org.novasparkle.sateproxy.auth.ActiveLoginSession;
import org.novasparkle.sateproxy.auth.sql.AsyncExecutor;
import org.novasparkle.sateproxy.auth.sql.AuthSqlManager;
import org.novasparkle.sateproxy.auth.telegram.Bot;
import org.novasparkle.sateproxy.command.FindCommand;
import org.novasparkle.sateproxy.command.LoginCommand;
import org.novasparkle.sateproxy.command.RegisterCommand;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Plugin(
        id = "sateproxy",
        name = "SateProxy",
        version = "1.0",
        authors = {"NovaSparkle"},
        dependencies = {
                @Dependency(id = "limboapi")
        }
)
@Getter
public class SateProxy {

    public static SateProxy instance;

    private final File dataDirectory;
    private final Logger logger;
    private final ProxyServer proxyServer;

    private final LimboFactory limboFactory;
    private Limbo limbo;
    private VirtualWorld virtualWorld;
    private final File configFile;
    private YamlConfiguration config;
    private final AsyncExecutor asyncExecutor;

    private final Map<Player, ActiveLoginSession> sessionMap;

    private final Scheduler scheduler;

    private final Bot telegramBot;

    @Inject
    public SateProxy(Logger logger, ProxyServer proxyServer, @DataDirectory Path directory) {
        instance = this;
        this.sessionMap = new ConcurrentHashMap<>();
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.scheduler = proxyServer.getScheduler();
        this.dataDirectory = directory.toFile();
        this.limboFactory = this.proxyServer.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).filter(i -> i instanceof LimboFactory).map(i -> ((LimboFactory) i)).orElseThrow();
        this.configFile = new File(dataDirectory, "config.yml");
        this.saveResource("config.yml");
        this.saveResource("telegram.yml");
        this.asyncExecutor = new AsyncExecutor(this.getConfig().getConfigurationSection("mysql"));
        AuthSqlManager.initialize();

        this.telegramBot = new Bot();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        CommandManager commandManager = this.getProxyServer().getCommandManager();
        commandManager.register(commandManager.metaBuilder("find").plugin(this).build(), new FindCommand());
        commandManager.register(commandManager.metaBuilder("login").aliases("l").plugin(this).build(), new LoginCommand());
        commandManager.register(commandManager.metaBuilder("register").aliases("reg").plugin(this).build(), new RegisterCommand());

        this.virtualWorld = limboFactory.createVirtualWorld(Dimension.THE_END, 0, 0, 0, 0, 0);
        this.limbo = limboFactory.createLimbo(virtualWorld).setName("Auth").setShouldRespawn(false).setShouldUpdateTags(false);

        this.registerBot();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        asyncExecutor.shutdown();
        sessionMap.values().forEach(ActiveLoginSession::cancelAll);
    }

    @Subscribe(priority = 1)
    public void onLogin(LoginLimboRegisterEvent event) {
        Player player = event.getPlayer();
        event.addOnJoinCallback(() -> this.sendPlayer(player));
        if (AuthSqlManager.isRegistered(player)) {
            AuthSqlManager.onLogin(player);
        }
    }

    @SneakyThrows
    private void registerBot() {
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(telegramBot.getToken(), telegramBot);
    }

    public void sendPlayer(Player player) {
        ActiveLoginSession session = new ActiveLoginSession(virtualWorld);
        sessionMap.put(player, session);
        limbo.spawnPlayer(player, session);
    }

    public YamlConfiguration getConfig() {
        if (config == null) {
            this.reloadConfig();
        }
        return config;
    }

    @SneakyThrows
    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);

        final InputStream in = this.getResource("config.yml");
        if (in == null) {
            return;
        }

        this.config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, Charsets.UTF_8)));
    }

    @Nullable
    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = this.getClass().getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public void saveResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = this.getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(dataDirectory, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataDirectory, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                logger.info("Could not save {} to {} because {} already exists.", outFile.getName(), outFile, outFile.getName());
            }
        } catch (IOException ex) {
            logger.error("Could not save {} to {}", outFile.getName(), outFile, ex);
        }
    }
}
