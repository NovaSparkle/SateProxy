package org.novasparkle.sateproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.auth.ActiveLoginSession;
import org.novasparkle.sateproxy.auth.sql.AuthSqlManager;
import org.novasparkle.sateproxy.configuration.ConfigManager;

import java.util.Optional;

public class LoginCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource commandSource = invocation.source();
        if (commandSource instanceof Player player) {
            String[] args = invocation.arguments();

            if (AuthSqlManager.isRegistered(player)) {
                if (AuthSqlManager.isCorrect(player, args[0])) {
                    Optional<RegisteredServer> mainServer = SateProxy.instance.getProxyServer().getServer(ConfigManager.getString("auth.sendAfterLogin"));

                    if (mainServer.isPresent()) {
                        player.createConnectionRequest(mainServer.get()).fireAndForget();
                    } else {
                        ConfigManager.sendMessage(player, "login.targetServerNotFound");
                    }

                } else {
                    ActiveLoginSession session = SateProxy.instance.getSessionMap().get(player);
                    if (session == null)
                        throw new RuntimeException("Игрока исполнявшего команду /login не в карте активных сессий!");
                    session.getAuthAttemptsLeft().decrementAndGet();
                    ConfigManager.sendMessage(player, "login.incorrectPassword", "attempts-%-" + session.getAuthAttemptsLeft());
                }
            } else {
                ConfigManager.sendMessage(player, "login.unregistered");
            }
        }
    }
}
