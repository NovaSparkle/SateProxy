package org.novasparkle.sateproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.configuration.ConfigManager;

public class FindCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length < 1) return;

        CommandSource source = invocation.source();
        Player player = SateProxy.instance.getProxyServer().getPlayer(args[0]).orElse(null);

        if (player == null) {
            ConfigManager.sendMessage(source, "playerOffline", "player-%-" + args[0]);
            return;
        }

        ServerConnection connection = player.getCurrentServer().orElse(null);
        if (connection == null) {
            ConfigManager.sendMessage(source, "playerOffline", "player-%-" + args[0]);
            return;
        }

        RegisteredServer server = connection.getServer();

        String serverName = server.getServerInfo().getName();
        String translate = ConfigManager.getString("localization." + serverName);
        ConfigManager.sendMessage(player, "playerServer", "player-%-" + args[0], "server-%-" + translate);

    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(ConfigManager.getString("permissions.commands.find"));
    }
}
