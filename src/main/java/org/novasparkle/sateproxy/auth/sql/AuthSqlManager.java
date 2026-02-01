package org.novasparkle.sateproxy.auth.sql;

import com.velocitypowered.api.proxy.Player;
import lombok.experimental.UtilityClass;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.configuration.ConfigManager;
import org.novasparkle.sateproxy.utilities.Utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@UtilityClass
public class AuthSqlManager {
    private AsyncExecutor asyncExecutor;
    private DateTimeFormatter formatter;
    private String tableName;
    private final String NICKNAME = "NICKNAME";
    private final String LOWERCASE_NICK = "LOWERCASE_NICK";
    private final String UUID = "UUID";
    private final String HASH = "HASH";
    private final String REGISTER_IP = "REGISTER_IP";
    private final String REGISTER_DATE = "REGISTER_DATE";
    private final String LAST_LOGIN_IP = "LAST_LOGIN_IP";
    private final String LAST_LOGIN_DATE = "LAST_LOGIN_DATE";

    public void initialize() {
        asyncExecutor = SateProxy.instance.getAsyncExecutor();
        tableName = ConfigManager.getString("auth.tableName");
        formatter = new DateTimeFormatterBuilder().appendPattern(ConfigManager.getString("auth.dateTimeFormat")).toFormatter();
        createTable();
    }

    public void createTable() {
        asyncExecutor.executeAsync(String.format("CREATE TABLE IF NOT EXISTS %s" +
                "(ID int PRIMARY KEY NOT NULL AUTO_INCREMENT," +
                "%s varchar(55), " +        // NICKNAME
                "%s varchar(55), " +       // LOWERCASE_NICK
                "%s varchar(75), " +      // UUID
                "%s varchar(99), " +     // HASH
                "%s varchar(25), " +    // REGISTER_IP
                "%s varchar(25), " +   // REGISTER_DATE
                "%s varchar(25), " +  // LAST_LOGIN_IP
                "%s varchar(25)), ", // LAST_LOGIN_DATE
                tableName, NICKNAME, LOWERCASE_NICK, UUID, HASH, REGISTER_IP, REGISTER_DATE, LAST_LOGIN_IP, LAST_LOGIN_DATE));
    }

    public void onRegister(Player player, String password) {
        String passwordHash = Utils.getHash(password);
        String ipv4 = player.getRemoteAddress().getAddress().getHostAddress();
        String timestamp = LocalDateTime.now().format(formatter);

        asyncExecutor.executeSync(String.format("INSERT INTO %s " +
                        "(%s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                tableName,
                NICKNAME,
                LOWERCASE_NICK,
                UUID,
                HASH,
                REGISTER_IP,
                REGISTER_DATE,
                LAST_LOGIN_IP,
                LAST_LOGIN_DATE),
                player.getUsername(),
                player.getUsername().toLowerCase(),
                player.getUniqueId().toString(),
                passwordHash,
                ipv4,
                timestamp,
                ipv4,
                timestamp
        );
    }

    public void onLogin(Player player) {
        String loginIP = player.getRemoteAddress().getAddress().getHostAddress();
        String timestamp = LocalDateTime.now().format(formatter);

        asyncExecutor.executeAsync(String.format("UPDATE %s SET %s = ?, %s = ? WHERE %s = ?;", tableName, LAST_LOGIN_IP, LAST_LOGIN_DATE, UUID), loginIP, timestamp, player.getUniqueId().toString());
    }


    public boolean isCorrect(Player player, String password) {
        String hash = asyncExecutor.executeQuery(String.format("SELECT %s FROM %s WHERE %s = ?;", HASH, tableName, UUID), (rs) -> rs.getString(HASH), player.getUniqueId().toString()).get(0);
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        return Utils.checkPassword(password, hash);
    }

    public boolean isRegistered(Player player) {
        return !asyncExecutor.executeQuery(String.format("SELECT %s FROM %s WHERE %s = ?;", NICKNAME, tableName, UUID), (rs) -> rs.getString(NICKNAME), player.getUniqueId().toString()).isEmpty();
    }


}
