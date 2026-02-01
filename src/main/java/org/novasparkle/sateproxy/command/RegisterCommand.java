package org.novasparkle.sateproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.novasparkle.sateproxy.auth.sql.AuthSqlManager;
import org.novasparkle.sateproxy.configuration.ConfigManager;

import java.util.List;
import java.util.regex.Pattern;

public class RegisterCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        System.out.println(source.getClass());
        String[] args = invocation.arguments();
        if (args.length < 1) {
            ConfigManager.sendMessage(source, "lowArguments");
            return;
        }
        if (source instanceof Player player) {
            if (AuthSqlManager.isRegistered(player)) {
                ConfigManager.sendMessage(player, "register.alreadyRegistered");
                return;
            }

            int minLength = ConfigManager.getInt("auth.password.minLength");
            List<String> unsafePasswords = null;
            if (ConfigManager.getBoolean("auth.password.unsafePasswords.enable")) {
                unsafePasswords = ConfigManager.getStringList("auth.password.unsafePasswords.list");
            }

            String passwordRegex = ConfigManager.getString("auth.password.regex");
            Pattern pattern = Pattern.compile(passwordRegex);

            String password = invocation.arguments()[1];
            if (password.length() < minLength) {
                ConfigManager.sendMessage(player, "register.lowPassword");

            } else if (unsafePasswords != null && unsafePasswords.contains(password)) {
                ConfigManager.sendMessage(player, "register.unsafePassword");

            } else if (!pattern.matcher(password).matches()) {
                ConfigManager.sendMessage(player, "register.invalidSymbols");

            } else {
                AuthSqlManager.onRegister(player, password);
                ConfigManager.sendMessage(player, "register.successfulRegister");
            }
        }
    }
}
